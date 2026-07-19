package com.thebook.llmtranslate.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.omegat.util.Log;
import org.omegat.util.Preferences;

public final class PluginLogger {

    public static final String PREF_LOGGING = "llm.logging.enabled";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static volatile boolean enabled = false;

    private PluginLogger() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean e) {
        enabled = e;
        Preferences.setPreference(PREF_LOGGING, e ? "true" : "false");
    }

    public static void loadFromPrefs() {
        enabled = "true".equals(Preferences.getPreferenceDefault(PREF_LOGGING, "false"));
    }

    private static String ts() {
        return LocalDateTime.now().format(FMT);
    }

    public static void log(String msg) {
        if (enabled) {
            Log.log("[" + ts() + "] " + msg);
        }
    }

    public static void logError(String msg) {
        Log.log("[" + ts() + "] " + msg);
    }

    public static void logErrorRB(Exception e, String msg) {
        Log.logErrorRB(e, msg);
    }
}
