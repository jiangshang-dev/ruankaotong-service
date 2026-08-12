package com.heima.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ruankao.ai")
public class AiProperties {

    private String baseUrl = "http://127.0.0.1:8000/v1";
    private String modelName = "qwen";
    private String apiKey = "sk-local";
    private boolean stream = false;
    private String polishPrompt = "classpath:prompts/ruankao-polish-system.txt";
    private String scorePrompt = "classpath:prompts/ruankao-score-system.txt";
    private int timeoutSeconds = 180;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public String getPolishPrompt() {
        return polishPrompt;
    }

    public void setPolishPrompt(String polishPrompt) {
        this.polishPrompt = polishPrompt;
    }

    public String getScorePrompt() {
        return scorePrompt;
    }

    public void setScorePrompt(String scorePrompt) {
        this.scorePrompt = scorePrompt;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
