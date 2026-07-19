package com.thebook.llmtranslate;

import java.awt.GridBagConstraints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.omegat.core.Core;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.core.data.TMXEntry;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.gui.glossary.GlossaryEntry;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.Language;
import org.omegat.util.Log;
import org.omegat.util.Preferences;

import com.thebook.llmtranslate.util.PluginLogger;

import com.thebook.llmtranslate.config.PluginConfig;
import com.thebook.llmtranslate.config.ProviderPreset;
import com.thebook.llmtranslate.project.ProjectPrompt;
import com.thebook.llmtranslate.service.AIServiceManager;

public class LLMTranslate extends BaseCachedTranslate {

    public LLMTranslate() {
        PluginLogger.log("LLM Translate: instance created, preference key=" + getPreferenceName());
    }

    @Override
    protected String getPreferenceName() {
        return "allow_llm_translate";
    }

    @Override
    public String getName() {
        return "LLM Translate (ai4j)";
    }

    @Override
    public boolean isConfigurable() {
        PluginLogger.log("LLM Translate: isConfigurable() = true");
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        PluginLogger.log("LLM Translate: setEnabled(" + enabled + ")");
        super.setEnabled(enabled);
    }

    private PluginConfig loadConfig() {
        PluginConfig cfg = new PluginConfig();
        String providerName = Preferences.getPreferenceDefault(PluginConfig.PREF_PROVIDER, "OpenAI");
        cfg.setProvider(ProviderPreset.fromDisplayName(providerName));
        cfg.setApiHost(Preferences.getPreferenceDefault(PluginConfig.PREF_API_HOST,
                cfg.getProvider().getDefaultHost()));
        cfg.setApiKey(getCredential(PluginConfig.PREF_API_KEY));
        cfg.setModel(Preferences.getPreferenceDefault(PluginConfig.PREF_MODEL,
                cfg.getProvider().getDefaultModel()));
        String temp = Preferences.getPreferenceDefault(PluginConfig.PREF_TEMPERATURE, "0");
        try {
            cfg.setTemperature(Double.parseDouble(temp));
        } catch (NumberFormatException e) {
            cfg.setTemperature(0);
        }
        cfg.setExtraParams(Preferences.getPreferenceDefault(PluginConfig.PREF_EXTRA_PARAMS, ""));
        cfg.setSystemPrompt(Preferences.getPreferenceDefault(
                PluginConfig.PREF_SYSTEM_PROMPT, PluginConfig.DEFAULT_SYSTEM_PROMPT));
        cfg.setUserPrompt(Preferences.getPreferenceDefault(
                PluginConfig.PREF_USER_PROMPT, PluginConfig.DEFAULT_USER_PROMPT));
        cfg.setUseCache(Preferences.getPreferenceDefault(PluginConfig.PREF_USE_CACHE, "true").equals("true"));
        cfg.setStoreTemporarily(isCredentialStoredTemporarily(PluginConfig.PREF_API_KEY));
        return cfg;
    }

    private SourceTextEntry findEntryByText(String text) {
        try {
            SourceTextEntry current = Core.getEditor().getCurrentEntry();
            if (current != null && text.equals(current.getSrcText())) {
                PluginLogger.log("LLM Translate: findEntryByText fast path HIT, file="
                        + current.getKey().file + ", entryNum=" + current.entryNum());
                return current;
            }
            PluginLogger.log("LLM Translate: findEntryByText fast path miss"
                    + (current == null ? " (no current entry)" : " (text mismatch)")
                    + ", fallback to scan all entries");
            for (SourceTextEntry entry : Core.getProject().getAllEntries()) {
                if (entry.getSrcText().equals(text)) {
                    PluginLogger.log("LLM Translate: findEntryByText fallback found, file="
                            + entry.getKey().file + ", entryNum=" + entry.entryNum());
                    return entry;
                }
            }
            PluginLogger.log("LLM Translate: findEntryByText not found for text="
                    + text.substring(0, Math.min(50, text.length())));
        } catch (Exception e) {
            PluginLogger.log("LLM Translate: findEntryByText error: " + e.getMessage());
        }
        return null;
    }

    private String getFileSegments(String filePath) {
        try {
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (SourceTextEntry entry : Core.getProject().getAllEntries()) {
                if (entry.getKey().file != null && entry.getKey().file.equals(filePath)) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(entry.getSrcText());
                    count++;
                }
            }
            PluginLogger.log("LLM Translate: getFileSegments file=" + filePath + ", segments=" + count
                    + ", totalLength=" + sb.length());
            return sb.toString();
        } catch (Exception e) {
            PluginLogger.log("LLM Translate: getFileSegments error: " + e.getMessage());
            return "";
        }
    }

    private String resolvePlaceholders(String template, Language sLang, Language tLang,
                                       String text, String projectPrompt, String glossary) {
        if (template == null || template.isEmpty()) return template;
        String result = template;
        boolean hasPlaceholders = false;
        SourceTextEntry ste = findEntryByText(text);

        if (result.contains("{sourceLang}")) { hasPlaceholders = true; result = result.replace("{sourceLang}", sLang.getLanguageCode()); }
        if (result.contains("{targetLang}")) { hasPlaceholders = true; result = result.replace("{targetLang}", tLang.getLanguageCode()); }
        if (result.contains("{sourceLangName}")) { hasPlaceholders = true; result = result.replace("{sourceLangName}", sLang.getDisplayName()); }
        if (result.contains("{targetLangName}")) { hasPlaceholders = true; result = result.replace("{targetLangName}", tLang.getDisplayName()); }
        if (result.contains("{text}")) { hasPlaceholders = true; result = result.replace("{text}", text); }
        if (result.contains("{glossary}")) { hasPlaceholders = true; result = result.replace("{glossary}", glossary != null ? glossary : ""); }
        if (result.contains("{projectPrompt}")) { hasPlaceholders = true; result = result.replace("{projectPrompt}", projectPrompt != null ? projectPrompt : ""); }
        if (result.contains("{prevSegment}")) { hasPlaceholders = true; result = result.replace("{prevSegment}", ste != null && ste.getKey().prev != null ? ste.getKey().prev : ""); }
        if (result.contains("{nextSegment}")) { hasPlaceholders = true; result = result.replace("{nextSegment}", ste != null && ste.getKey().next != null ? ste.getKey().next : ""); }
        if (result.contains("{segmentComment}")) { hasPlaceholders = true; result = result.replace("{segmentComment}", ste != null ? ste.getComment() != null ? ste.getComment() : "" : ""); }
        if (result.contains("{fileSegments}")) { hasPlaceholders = true; String fs = ste != null ? getFileSegments(ste.getKey().file) : ""; result = result.replace("{fileSegments}", fs); }
        try {
            if (ste != null) {
                TMXEntry info = Core.getProject().getTranslationInfo(ste);
                if (result.contains("{existingTranslation}")) { hasPlaceholders = true; result = result.replace("{existingTranslation}", info.isTranslated() ? info.translation : ""); }
                if (result.contains("{translationNote}")) { hasPlaceholders = true; result = result.replace("{translationNote}", info.hasNote() ? info.note : ""); }
                if (result.contains("{projectName}")) { hasPlaceholders = true; result = result.replace("{projectName}", Core.getProject().getProjectProperties().getProjectName()); }
            } else {
                if (result.contains("{existingTranslation}")) result = result.replace("{existingTranslation}", "");
                if (result.contains("{translationNote}")) result = result.replace("{translationNote}", "");
                if (result.contains("{projectName}")) result = result.replace("{projectName}", Core.getProject().getProjectProperties().getProjectName());
            }
        } catch (Exception e) {
            PluginLogger.log("LLM Translate: resolvePlaceholders error: " + e.getMessage());
        }
        if (hasPlaceholders) {
            PluginLogger.log("LLM Translate: resolvePlaceholders done, templateLength=" + template.length()
                    + ", resultLength=" + result.length());
        }
        return result;
    }

    @Override
    public void showConfigurationUI(Window parent) {
        PluginConfig cfg = loadConfig();

        JPanel panel = new JPanel(new java.awt.GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new java.awt.Insets(2, 0, 4, 5);

        int row = 0;

        // Provider preset
        JComboBox<String> providerCombo = new JComboBox<>(ProviderPreset.displayNames());
        providerCombo.setSelectedItem(cfg.getProvider().getDisplayName());
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        panel.add(new JLabel("Provider:"), c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(providerCombo, c);
        row++;

        // API Host
        JTextField hostField = new JTextField(cfg.getApiHost(), 40);
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        panel.add(new JLabel("API Host:"), c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(hostField, c);
        row++;

        // API Key
        JTextField apiKeyField = new JTextField(getCredential(PluginConfig.PREF_API_KEY), 40);
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        panel.add(new JLabel("API Key:"), c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(apiKeyField, c);
        row++;

        // Model
        JTextField modelField = new JTextField(cfg.getModel());
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        panel.add(new JLabel("Model:"), c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(modelField, c);
        row++;

        // Temperature
        JTextField tempField = new JTextField(String.valueOf(cfg.getTemperature()), 40);
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        panel.add(new JLabel("Temperature:"), c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(tempField, c);
        row++;

        // Enable logging
        JCheckBox loggingCheck = new JCheckBox("Enable verbose logging");
        loggingCheck.setSelected(PluginLogger.isEnabled());
        loggingCheck.addActionListener(ev -> PluginLogger.setEnabled(loggingCheck.isSelected()));
        c.gridx = 0; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        panel.add(loggingCheck, c);
        row++;

        // Use cache
        JCheckBox cacheCheck = new JCheckBox("Use cache");
        cacheCheck.setSelected(cfg.isUseCache());
        cacheCheck.addActionListener(ev -> Preferences.setPreference(
                PluginConfig.PREF_USE_CACHE, cacheCheck.isSelected() ? "true" : "false"));
        c.gridx = 0; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(cacheCheck, c);
        row++;

        // Extra Params
        JTextArea extraField = new JTextArea(3, 40);
        extraField.setText(cfg.getExtraParams());
        extraField.setLineWrap(true);
        extraField.setWrapStyleWord(true);
        JScrollPane extraScroll = new JScrollPane(extraField);
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel("Extra Params\n(JSON):"), c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0; c.weighty = 0.3;
        panel.add(extraScroll, c);
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0; c.weighty = 0;
        row++;

        // Placeholder dropdown
        String[] placeholderItems = {
            "(select placeholder to copy)",
            "{sourceLang}  | Source language code",
            "{targetLang}  | Target language code",
            "{sourceLangName}  | Source language display name",
            "{targetLangName}  | Target language display name",
            "{text}  | Current source segment text",
            "{prevSegment}  | Previous segment source text",
            "{nextSegment}  | Next segment source text",
            "{fileSegments}  | All source texts in current file",
            "{glossary}  | Matching glossary entries",
            "{segmentComment}  | Segment comment",
            "{projectName}  | Project name",
            "{projectPrompt}  | Project-level prompt",
            "{existingTranslation}  | Existing translation",
            "{translationNote}  | Translation note",
        };
        JComboBox<String> phCombo = new JComboBox<>(placeholderItems);
        phCombo.addActionListener(ev -> {
            String sel = (String) phCombo.getSelectedItem();
            if (sel == null || sel.startsWith("(")) return;
            String ph = sel.split("  ")[0];
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(ph), null);
            phCombo.setSelectedIndex(0);
        });
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        panel.add(new JLabel("Placeholder:"), c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(phCombo, c);
        row++;

        // System Prompt Template
        JTextArea systemField = new JTextArea(5, 40);
        systemField.setText(cfg.getSystemPrompt());
        systemField.setLineWrap(true);
        systemField.setWrapStyleWord(true);
        JScrollPane systemScroll = new JScrollPane(systemField);
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        JLabel systemLabel = new JLabel("<html>System Prompt Template<br>"
                + "<small>{sourceLang} {targetLang} {text} {glossary} {projectPrompt}</small></html>");
        panel.add(systemLabel, c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0; c.weighty = 0.4;
        panel.add(systemScroll, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0; c.weighty = 0;
        row++;

        // User Prompt Template
        JTextArea userField = new JTextArea(3, 40);
        userField.setText(cfg.getUserPrompt());
        userField.setLineWrap(true);
        userField.setWrapStyleWord(true);
        JScrollPane userScroll = new JScrollPane(userField);
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        JLabel userLabel = new JLabel("<html>User Prompt Template<br>"
                + "<small>{text} {sourceLang} {prevSegment} {nextSegment} {glossary}</small></html>");
        panel.add(userLabel, c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0; c.weighty = 0.3;
        panel.add(userScroll, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0; c.weighty = 0;
        row++;

        // Project prompt
        String projectPromptText = ProjectPrompt.get();
        boolean hasProject = Core.getProject() != null && Core.getProject().isProjectLoaded();
        JLabel projectLabel = new JLabel("Project Prompt\n(append):");
        JTextArea projectField = new JTextArea(3, 40);
        projectField.setText(hasProject
                ? (projectPromptText != null ? projectPromptText : "")
                : "(Open a project to edit project-level prompt)");
        projectField.setLineWrap(true);
        projectField.setWrapStyleWord(true);
        JScrollPane projectScroll = new JScrollPane(projectField);
        if (!hasProject) {
            projectField.setEnabled(false);
            projectField.setDisabledTextColor(java.awt.Color.GRAY);
        }
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        panel.add(projectLabel, c);
        c.gridx = 1; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0; c.weighty = 0.3;
        panel.add(projectScroll, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0; c.weighty = 0;
        row++;

        // Test connection button
        JButton testButton = new JButton("Test Connection");
        JPanel testPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        testPanel.add(testButton);
        c.gridx = 0; c.gridy = row; c.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(testPanel, c);

        testButton.addActionListener(e -> {
            PluginLogger.log("LLM Translate: test connection button clicked, host="
                    + hostField.getText().trim() + ", model=" + modelField.getText().trim());
            testButton.setEnabled(false);
            testButton.setText("Testing...");
            PluginConfig testCfg = new PluginConfig();
            testCfg.setApiHost(hostField.getText().trim());
            testCfg.setApiKey(apiKeyField.getText());
            testCfg.setModel(modelField.getText().trim());
            try {
                testCfg.setTemperature(Double.parseDouble(tempField.getText().trim()));
            } catch (NumberFormatException ex) {
                testCfg.setTemperature(0);
            }
            PluginConfig finalCfg = testCfg;
            new Thread(() -> {
                try {
                    AIServiceManager ai = new AIServiceManager(finalCfg);
                    long start = System.currentTimeMillis();
                    String result = ai.testConnection();
                    long elapsed = System.currentTimeMillis() - start;
                    PluginLogger.log("LLM Translate: test connection completed in " + elapsed + "ms");
                    SwingUtilities.invokeLater(() -> {
                        testButton.setEnabled(true);
                        testButton.setText("Test Connection");
                        if (result != null) {
                            PluginLogger.log("LLM Translate: test connection SUCCESS, response="
                                    + result.substring(0, Math.min(100, result.length())));
                            JOptionPane.showMessageDialog(panel,
                                    "Connection successful! (" + elapsed + "ms)\n\nResponse:\n"
                                            + result.substring(0, Math.min(200, result.length())),
                                    "Test Result", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            PluginLogger.log("LLM Translate: test connection FAILED: empty response");
                            JOptionPane.showMessageDialog(panel,
                                    "Connection failed: empty response",
                                    "Test Result", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (Exception ex) {
                    PluginLogger.log("LLM Translate: test connection FAILED: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        testButton.setEnabled(true);
                        testButton.setText("Test Connection");
                        JOptionPane.showMessageDialog(panel,
                                "Connection failed:\n" + ex.getMessage(),
                                "Test Result", JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        });

        // Preset change listener
        providerCombo.addActionListener(e -> {
            ProviderPreset preset = ProviderPreset.fromDisplayName(
                    (String) providerCombo.getSelectedItem());
            hostField.setText(preset.getDefaultHost());
            modelField.setText(preset.getDefaultModel());
        });

        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                ProviderPreset preset = ProviderPreset.fromDisplayName(
                        (String) providerCombo.getSelectedItem());
                Preferences.setPreference(PluginConfig.PREF_PROVIDER, preset.getDisplayName());
                Preferences.setPreference(PluginConfig.PREF_API_HOST, hostField.getText().trim());
                Preferences.setPreference(PluginConfig.PREF_MODEL, modelField.getText().trim());
                Preferences.setPreference(PluginConfig.PREF_TEMPERATURE, tempField.getText().trim());
                Preferences.setPreference(PluginConfig.PREF_EXTRA_PARAMS, extraField.getText().trim());
                Preferences.setPreference(PluginConfig.PREF_SYSTEM_PROMPT, systemField.getText().trim());
                Preferences.setPreference(PluginConfig.PREF_USER_PROMPT, userField.getText().trim());
                setCredential(PluginConfig.PREF_API_KEY, apiKeyField.getText(),
                        panel.temporaryCheckBox.isSelected());
                ProjectPrompt.save(projectField.getText().trim());
                PluginLogger.log("LLM Translate: configuration saved, provider=" + preset.getDisplayName()
                        + ", host=" + hostField.getText().trim());
            }
        };
        dialog.panel.valueLabel1.setVisible(false);
        dialog.panel.valueField1.setVisible(false);
        dialog.panel.valueLabel2.setVisible(false);
        dialog.panel.valueField2.setVisible(false);
        dialog.panel.itemsPanel.add(panel);
        dialog.show();
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        String srcLang = sLang.getLanguageCode();
        String tgtLang = tLang.getLanguageCode();
        PluginLogger.log("LLM Translate: translate() called - " + srcLang + " -> " + tgtLang
                + ", text length=" + text.length());

        PluginConfig cfg = loadConfig();

        if (cfg.isUseCache()) {
            String cached = getFromCache(sLang, tLang, text);
            if (cached != null) {
                PluginLogger.log("LLM Translate: cache HIT for text=" + text.substring(0, Math.min(50, text.length())));
                return cached;
            }
            PluginLogger.log("LLM Translate: cache MISS for text=" + text.substring(0, Math.min(50, text.length())));
        } else {
            PluginLogger.log("LLM Translate: cache disabled");
        }
        PluginLogger.log("LLM Translate: using provider=" + cfg.getProvider().getDisplayName()
                + ", model=" + cfg.getModel() + ", host=" + cfg.getApiHost());

        String rawProjectPrompt = ProjectPrompt.get();
        PluginLogger.log("LLM Translate: raw project prompt="
                + (rawProjectPrompt != null ? rawProjectPrompt.substring(0, Math.min(100, rawProjectPrompt.length())) : "null"));

        String projectPrompt = resolvePlaceholders(rawProjectPrompt,
                sLang, tLang, text, null, "");

        StringBuilder glossaryBuilder = new StringBuilder();
        try {
            List<GlossaryEntry> entries = Core.getGlossaryManager().getGlossaryEntries(text);
            PluginLogger.log("LLM Translate: glossary entries found=" + (entries != null ? entries.size() : 0));
            if (entries != null && !entries.isEmpty()) {
                for (GlossaryEntry ge : entries) {
                    String src = ge.getSrcText();
                    String[] targets = ge.getLocTerms(false);
                    if (targets.length > 0) {
                        glossaryBuilder.append(src).append("\t").append(targets[0]).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            PluginLogger.log("LLM Translate: glossary error: " + e.getMessage());
        }
        String glossary = glossaryBuilder.toString();
        if (!glossary.isEmpty()) {
            PluginLogger.log("LLM Translate: glossary built, length=" + glossary.length());
        }

        String systemPrompt = resolvePlaceholders(cfg.getSystemPrompt(),
                sLang, tLang, text, projectPrompt, glossary);
        String userPrompt = resolvePlaceholders(cfg.getUserPrompt(),
                sLang, tLang, text, projectPrompt, glossary);

        PluginLogger.log("LLM Translate: systemPrompt empty=" + (systemPrompt == null || systemPrompt.isEmpty())
                + ", userPrompt empty=" + (userPrompt == null || userPrompt.isEmpty()));

        long start = System.currentTimeMillis();
        try {
            AIServiceManager ai = new AIServiceManager(cfg);
            String result = ai.translate(systemPrompt, userPrompt, text);

            if (result == null || result.isEmpty()) {
                PluginLogger.log("LLM Translate: translation returned empty result");
                return null;
            }

            long elapsed = System.currentTimeMillis() - start;
            PluginLogger.log("LLM Translate: translation SUCCESS in " + elapsed + "ms, "
                    + "result length=" + result.length());
            if (cfg.isUseCache()) {
                putToCache(sLang, tLang, text, result);
            }
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            PluginLogger.logErrorRB(e, "LLM Translate: translation FAILED in " + elapsed + "ms: " + e.getMessage());
            throw e;
        }
    }
}
