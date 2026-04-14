package com.cariesguard.image.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "caries.image.storage")
public class ImageStorageProperties {

    private String localRoot;
    private String bucketName = "caries-image";
    private String providerCode = "MINIO";
    private String publicBaseUrl = "http://127.0.0.1:8080";
    private String accessUrlSecret = "change-me-to-a-strong-image-access-secret";
    private long accessUrlExpireSeconds = 900;
    private final Minio minio = new Minio();

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
        return normalizeProviderCode(providerCode);
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
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

    public Minio getMinio() {
        return minio;
    }

    private String normalizeProviderCode(String value) {
        if (!StringUtils.hasText(value)) {
            return "MINIO";
        }
        return value.trim().replace('-', '_').toUpperCase();
    }

    public static class Minio {
        private String endpoint = "http://127.0.0.1:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private boolean secure = false;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }
    }
}
