package com.thebook.llmtranslate.config;

public class PluginConfig {

    private ProviderPreset provider = ProviderPreset.OPENAI;
    private String apiHost;
    private String apiKey;
    private String model;
    private double temperature;
    private String extraParams;
    private String systemPrompt;
    private String userPrompt;
    private boolean useCache;
    private boolean storeTemporarily;

    public static final String PREF_PROVIDER = "llm.provider";
    public static final String PREF_API_HOST = "llm.api.host";
    public static final String PREF_API_KEY = "llm.api.key";
    public static final String PREF_MODEL = "llm.model";
    public static final String PREF_TEMPERATURE = "llm.temperature";
    public static final String PREF_EXTRA_PARAMS = "llm.extra.params";
    public static final String PREF_SYSTEM_PROMPT = "llm.system.prompt";
    public static final String PREF_USER_PROMPT = "llm.user.prompt";
    public static final String PREF_USE_CACHE = "llm.use.cache";

    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are a professional translator. Translate from {sourceLang} to {targetLang}. "
            + "Preserve any tags in the text. Return ONLY the translation, no explanations.";
    public static final String DEFAULT_USER_PROMPT = "{text}";

    public PluginConfig() {
    }

    public PluginConfig(ProviderPreset provider, String apiHost, String apiKey,
                        String model, double temperature, String extraParams,
                        String systemPrompt, String userPrompt,
                        boolean useCache, boolean storeTemporarily) {
        this.provider = provider;
        this.apiHost = apiHost;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.extraParams = extraParams;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.useCache = useCache;
        this.storeTemporarily = storeTemporarily;
    }

    public ProviderPreset getProvider() {
        return provider;
    }

    public void setProvider(ProviderPreset provider) {
        this.provider = provider;
    }

    public String getApiHost() {
        return apiHost;
    }

    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(String extraParams) {
        this.extraParams = extraParams;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public boolean isStoreTemporarily() {
        return storeTemporarily;
    }

    public void setStoreTemporarily(boolean storeTemporarily) {
        this.storeTemporarily = storeTemporarily;
    }
}
