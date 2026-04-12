package com.cariesguard.image.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caries.image.storage")
public class ImageStorageProperties {

    private String localRoot;
    private String bucketName = "caries-image";
    private String providerCode = "MINIO";
    private String accessUrlSecret = "change-me-to-a-strong-image-access-secret";
    private long accessUrlExpireSeconds = 900;

    public String getLocalRoot() {
        return localRoot;
    }

    public void setLocalRoot(String localRoot) {
        this.localRoot = localRoot;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getAccessUrlSecret() {
        return accessUrlSecret;
    }

    public void setAccessUrlSecret(String accessUrlSecret) {
        this.accessUrlSecret = accessUrlSecret;
    }

    public long getAccessUrlExpireSeconds() {
        return accessUrlExpireSeconds;
    }

    public void setAccessUrlExpireSeconds(long accessUrlExpireSeconds) {
        this.accessUrlExpireSeconds = accessUrlExpireSeconds;
    }
}
