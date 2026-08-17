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
    private String caseSolvePrompt = "classpath:prompts/ruankao-case-solve-system.txt";
    private String caseScorePrompt = "classpath:prompts/ruankao-case-score-system.txt";
    private String guidePrompt = "classpath:prompts/ruankao-essay-guide-system.txt";
    /** 多模态识图模型；为空则回退到 modelName */
    private String visionModelName = "";
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

    public String getCaseSolvePrompt() {
        return caseSolvePrompt;
    }

    public void setCaseSolvePrompt(String caseSolvePrompt) {
        this.caseSolvePrompt = caseSolvePrompt;
    }

    public String getCaseScorePrompt() {
        return caseScorePrompt;
    }

    public void setCaseScorePrompt(String caseScorePrompt) {
        this.caseScorePrompt = caseScorePrompt;
    }

    public String getGuidePrompt() {
        return guidePrompt;
    }

    public void setGuidePrompt(String guidePrompt) {
        this.guidePrompt = guidePrompt;
    }

    public String getVisionModelName() {
        return visionModelName;
    }

    public void setVisionModelName(String visionModelName) {
        this.visionModelName = visionModelName;
    }

    /** 案例分析识图：优先 vision-model-name，否则使用主模型 */
    public String resolveVisionModelName() {
        if (visionModelName != null && !visionModelName.isBlank()) {
            return visionModelName.trim();
        }
        return modelName;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
