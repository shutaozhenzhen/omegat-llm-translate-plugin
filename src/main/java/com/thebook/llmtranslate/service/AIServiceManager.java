package com.thebook.llmtranslate.service;

import com.thebook.llmtranslate.util.PluginLogger;

import io.github.lnyocly.ai4j.config.OpenAiConfig;
import okhttp3.OkHttpClient;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletion;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatCompletionResponse;
import io.github.lnyocly.ai4j.platform.openai.chat.entity.ChatMessage;
import io.github.lnyocly.ai4j.service.Configuration;
import io.github.lnyocly.ai4j.service.IChatService;
import io.github.lnyocly.ai4j.service.PlatformType;
import io.github.lnyocly.ai4j.service.factory.AiService;

import com.thebook.llmtranslate.config.PluginConfig;

public class AIServiceManager {

    private final IChatService chatService;
    private final String apiKey;
    private final String fullChatUrl;
    private final String model;
    private final double temperature;

    public AIServiceManager(PluginConfig config) {
        this.apiKey = config.getApiKey();
        this.model = config.getModel();
        this.temperature = config.getTemperature();

        String host = config.getApiHost().trim();
        this.fullChatUrl = host;

        OpenAiConfig openAiConfig = new OpenAiConfig();
        openAiConfig.setApiKey(apiKey);
        openAiConfig.setApiHost(host);
        openAiConfig.setChatCompletionUrl("chat/completions");

        Configuration configuration = new Configuration();
        configuration.setOpenAiConfig(openAiConfig);
        configuration.setOkHttpClient(new OkHttpClient());

        AiService aiService = new AiService(configuration);
        this.chatService = aiService.getChatService(PlatformType.OPENAI);
    }

    public String testConnection() throws Exception {
        PluginLogger.log("AIServiceManager: testing connection, host=" + fullChatUrl + ", model=" + model);
        ChatCompletion request = ChatCompletion.builder()
                .model(model)
                .temperature(0f)
                .message(ChatMessage.withUser("Hello"))
                .build();

        PluginLogger.log("AIServiceManager: >>> request:\n" + request);
        ChatCompletionResponse response = chatService.chatCompletion(request);
        PluginLogger.log("AIServiceManager: <<< response:\n" + response);

        if (response != null
                && response.getChoices() != null
                && !response.getChoices().isEmpty()
                && response.getChoices().get(0).getMessage() != null
                && response.getChoices().get(0).getMessage().getContent() != null) {
            return response.getChoices().get(0).getMessage().getContent().getText();
        }
        return null;
    }

    public String translate(String systemPrompt, String userPrompt, String userText) throws Exception {
        String finalSystem = (systemPrompt == null || systemPrompt.isEmpty()) ? null : systemPrompt;
        String finalUser = (userPrompt == null || userPrompt.isEmpty()) ? userText : userPrompt;

        ChatCompletion.ChatCompletionBuilder builder = ChatCompletion.builder()
                .model(model)
                .temperature((float) temperature);

        if (finalSystem != null) {
            builder.message(ChatMessage.withSystem(finalSystem));
        }
        builder.message(ChatMessage.withUser(finalUser));

        ChatCompletion request = builder.build();

        PluginLogger.log("AIServiceManager: >>> host=" + fullChatUrl + ", model=" + model
                + "\nsystem=" + finalSystem + "\nuser=" + finalUser);
        ChatCompletionResponse response = chatService.chatCompletion(request);
        PluginLogger.log("AIServiceManager: <<< response=" + response);

        if (response != null
                && response.getChoices() != null
                && !response.getChoices().isEmpty()
                && response.getChoices().get(0).getMessage() != null
                && response.getChoices().get(0).getMessage().getContent() != null) {
            return response.getChoices().get(0).getMessage().getContent().getText();
        }
        return null;
    }
}
