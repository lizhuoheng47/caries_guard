package com.cariesguard.integration.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "caries.storage")
public class StorageProperties {

    private String provider = "MINIO";
    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String region;
    private boolean secure = false;
    private long defaultPresignExpireSeconds = 900;
    private boolean autoCreateBuckets = true;
    private String proxyAccessSecret = "change-me-to-a-strong-image-access-secret";
    private final Buckets buckets = new Buckets();

    public String getProvider() {
        return normalize(provider, "MINIO");
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public long getDefaultPresignExpireSeconds() {
        return defaultPresignExpireSeconds;
    }

    public void setDefaultPresignExpireSeconds(long defaultPresignExpireSeconds) {
        this.defaultPresignExpireSeconds = defaultPresignExpireSeconds;
    }

    public boolean isAutoCreateBuckets() {
        return autoCreateBuckets;
    }

    public void setAutoCreateBuckets(boolean autoCreateBuckets) {
        this.autoCreateBuckets = autoCreateBuckets;
    }

    public String getProxyAccessSecret() {
        return proxyAccessSecret;
    }

    public void setProxyAccessSecret(String proxyAccessSecret) {
        this.proxyAccessSecret = proxyAccessSecret;
    }

    public Buckets getBuckets() {
        return buckets;
    }

    public Duration defaultPresignExpireDuration() {
        return Duration.ofSeconds(Math.max(60, defaultPresignExpireSeconds));
    }

    public String bucketName(String bucketCode) {
        String normalized = normalize(bucketCode, "IMAGE");
        return switch (normalized) {
            case "VISUAL" -> buckets.getVisual();
            case "REPORT" -> buckets.getReport();
            case "EXPORT" -> buckets.getExport();
            case "IMAGE" -> buckets.getImage();
            default -> normalized.toLowerCase().startsWith("caries-") ? normalized.toLowerCase() : buckets.getImage();
        };
    }

    private String normalize(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        return value.trim().replace('-', '_').toUpperCase();
    }

    public static class Buckets {
        private String image = "caries-image";
        private String visual = "caries-visual";
        private String report = "caries-report";
        private String export = "caries-export";

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getVisual() {
            return visual;
        }

        public void setVisual(String visual) {
            this.visual = visual;
        }

        public String getReport() {
            return report;
        }

        public void setReport(String report) {
            this.report = report;
        }

        public String getExport() {
            return export;
        }

        public void setExport(String export) {
            this.export = export;
        }
    }
}
