package com.cariesguard.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caries.analysis")
public class AnalysisProperties {

    private String defaultModelVersion = "caries-v1";
    private String callbackSecret = "change-me-to-a-strong-analysis-callback-secret";
    private long callbackAllowedClockSkewSeconds = 300;

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
}
