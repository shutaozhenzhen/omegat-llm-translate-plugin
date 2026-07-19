package com.thebook.llmtranslate.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.omegat.core.Core;
import org.omegat.core.data.ProjectProperties;
import com.thebook.llmtranslate.util.PluginLogger;

public class ProjectPrompt {

    private static final String FILE_NAME = "llm-prompt.txt";
    private static String currentPrompt;

    public static String get() {
        return currentPrompt;
    }

    public static void load() {
        currentPrompt = null;
        try {
            ProjectProperties props = Core.getProject().getProjectProperties();
            if (props == null) return;
            File file = new File(props.getProjectInternalDir(), FILE_NAME);
            if (file.exists()) {
                currentPrompt = new String(Files.readAllBytes(file.toPath()), "UTF-8").trim();
                PluginLogger.log("ProjectPrompt: loaded from " + file.getAbsolutePath());
            } else {
                PluginLogger.log("ProjectPrompt: no project prompt file found");
            }
        } catch (Exception e) {
            PluginLogger.log("ProjectPrompt: failed to load: " + e.getMessage());
        }
    }

    public static void save(String text) {
        try {
            ProjectProperties props = Core.getProject().getProjectProperties();
            if (props == null) return;
            File file = new File(props.getProjectInternalDir(), FILE_NAME);
            if (text == null || text.trim().isEmpty()) {
                if (file.exists()) file.delete();
                currentPrompt = null;
                PluginLogger.log("ProjectPrompt: deleted");
            } else {
                Files.write(file.toPath(), text.trim().getBytes("UTF-8"),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                currentPrompt = text.trim();
                PluginLogger.log("ProjectPrompt: saved to " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            PluginLogger.log("ProjectPrompt: failed to save: " + e.getMessage());
        }
    }

    public static void clear() {
        currentPrompt = null;
    }
}
