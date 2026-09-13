package com.cariesguard.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caries.password-reset")
public class PasswordResetProperties {

    private boolean enabled = false;
    private boolean developmentCodeEnabled = false;
    private String deliveryMode = "NONE";
    private String mailFrom;
    private int codeTtlMinutes = 10;
    private int maxAttempts = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDevelopmentCodeEnabled() {
        return developmentCodeEnabled;
    }

    public void setDevelopmentCodeEnabled(boolean developmentCodeEnabled) {
        this.developmentCodeEnabled = developmentCodeEnabled;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public int getCodeTtlMinutes() {
        return codeTtlMinutes;
    }

    public void setCodeTtlMinutes(int codeTtlMinutes) {
        this.codeTtlMinutes = codeTtlMinutes;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
