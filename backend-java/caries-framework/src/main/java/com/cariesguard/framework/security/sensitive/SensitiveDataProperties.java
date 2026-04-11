package com.cariesguard.framework.security.sensitive;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caries.security.sensitive")
public class SensitiveDataProperties {

    private String cryptoSecret = "change-me-to-a-strong-sensitive-crypto-secret";
    private String hashSecret = "change-me-to-a-strong-sensitive-hash-secret";

    public String getCryptoSecret() {
        return cryptoSecret;
    }

    public void setCryptoSecret(String cryptoSecret) {
        this.cryptoSecret = cryptoSecret;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public void setHashSecret(String hashSecret) {
        this.hashSecret = hashSecret;
    }
}
