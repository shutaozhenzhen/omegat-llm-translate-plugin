package com.thebook.llmtranslate;

import static org.omegat.core.events.IProjectEventListener.PROJECT_CHANGE_TYPE;

import org.omegat.core.Core;
import org.omegat.core.CoreEvents;

import com.thebook.llmtranslate.project.ProjectPrompt;
import com.thebook.llmtranslate.util.PluginLogger;

public final class LLMTranslatePlugin {

    public static void loadPlugins() {
        PluginLogger.loadFromPrefs();
        PluginLogger.log("LLM Translate Plugin: loadPlugins() called");
        Core.registerMachineTranslationClass(LLMTranslate.class);
        PluginLogger.log("LLM Translate Plugin: registered LLMTranslate class");

        CoreEvents.registerProjectChangeListener(event -> {
            PluginLogger.log("LLM Translate Plugin: project event - " + event);
            switch (event) {
                case LOAD:
                case CREATE:
                    ProjectPrompt.load();
                    break;
                case CLOSE:
                    ProjectPrompt.clear();
                    break;
            }
        });
    }

    public static void unloadPlugins() {
        PluginLogger.logError("LLM Translate Plugin: unloadPlugins() called");
    }
}
