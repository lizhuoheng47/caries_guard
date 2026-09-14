package com.cariesguard.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caries.analysis")
public class AnalysisProperties {

    private String defaultModelVersion = "caries-v1";
    private String callbackSecret = "change-me-to-a-strong-analysis-callback-secret";
    private long callbackAllowedClockSkewSeconds = 300;
    private String inferenceBaseUrl = "http://127.0.0.1:8001";
    private String internalApiKey = "change-me-to-a-strong-internal-api-key";

    public String getDefaultModelVersion() {
        return defaultModelVersion;
    }

    public void setDefaultModelVersion(String defaultModelVersion) {
        this.defaultModelVersion = defaultModelVersion;
    }

    public String getCallbackSecret() {
        return callbackSecret;
    }

    public void setCallbackSecret(String callbackSecret) {
        this.callbackSecret = callbackSecret;
    }

    public long getCallbackAllowedClockSkewSeconds() {
        return callbackAllowedClockSkewSeconds;
    }

    public void setCallbackAllowedClockSkewSeconds(long callbackAllowedClockSkewSeconds) {
        this.callbackAllowedClockSkewSeconds = callbackAllowedClockSkewSeconds;
    }

    public String getInferenceBaseUrl() { return inferenceBaseUrl; }
    public void setInferenceBaseUrl(String inferenceBaseUrl) { this.inferenceBaseUrl = inferenceBaseUrl; }
    public String getInternalApiKey() { return internalApiKey; }
    public void setInternalApiKey(String internalApiKey) { this.internalApiKey = internalApiKey; }
}
