package com.thebook.llmtranslate.config;

public enum ProviderPreset {

    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    AZURE("Azure OpenAI", "https://{resource}.openai.azure.com", "gpt-4o"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", "deepseek-chat"),
    GROQ("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
    OLLAMA("Ollama", "http://localhost:11434", "llama3.2"),
    TOGETHER("Together AI", "https://api.together.xyz/v1", "mistralai/Mixtral-8x7B-Instruct-v0.1"),
    VLLM("vLLM", "http://localhost:8000/v1", ""),
    CUSTOM("Custom", "", "");

    private final String displayName;
    private final String defaultHost;
    private final String defaultModel;

    ProviderPreset(String displayName, String defaultHost, String defaultModel) {
        this.displayName = displayName;
        this.defaultHost = defaultHost;
        this.defaultModel = defaultModel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultHost() {
        return defaultHost;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public static ProviderPreset fromDisplayName(String name) {
        for (ProviderPreset p : values()) {
            if (p.displayName.equals(name)) {
                return p;
            }
        }
        return CUSTOM;
    }

    public static String[] displayNames() {
        ProviderPreset[] values = values();
        String[] names = new String[values.length - 1];
        for (int i = 0; i < values.length - 1; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }
}
