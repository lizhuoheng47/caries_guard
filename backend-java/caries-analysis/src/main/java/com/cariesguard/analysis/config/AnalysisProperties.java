package com.cariesguard.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caries.analysis")
public class AnalysisProperties {

    private String defaultModelVersion = "caries-v1";

    public String getDefaultModelVersion() {
        return defaultModelVersion;
    }

    public void setDefaultModelVersion(String defaultModelVersion) {
        this.defaultModelVersion = defaultModelVersion;
    }
}
