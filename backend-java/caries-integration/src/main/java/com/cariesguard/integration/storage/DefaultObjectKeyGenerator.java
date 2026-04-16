package com.cariesguard.integration.storage;

import java.time.LocalDate;
import java.util.Locale;
import org.springframework.util.StringUtils;

public class DefaultObjectKeyGenerator implements ObjectKeyGenerator {

    @Override
    public String generate(ObjectKeyRequest request) {
        String kind = upper(required(request.objectKindCode(), "objectKindCode"));
        return switch (kind) {
            case "RAW_IMAGE" -> rawImageKey(request);
            case "VISUAL" -> visualAssetKey(request);
            case "REPORT" -> reportKey(request);
            case "EXPORT" -> exportKey(request);
            default -> throw new IllegalArgumentException("Unsupported object kind: " + request.objectKindCode());
        };
    }

    private String rawImageKey(ObjectKeyRequest request) {
        LocalDate date = date(request);
        return "org/%s/case/%s/image/%s/%04d/%02d/%02d/%s/%s".formatted(
                id(request.orgId(), "orgId"),
                segment(request.caseNo(), "caseNo"),
                upperSegment(request.imageTypeCode(), "imageTypeCode"),
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                id(request.attachmentId(), "attachmentId"),
                fileName(request.originalFileName()));
    }

    private String visualAssetKey(ObjectKeyRequest request) {
        String ext = extension(request.originalFileName(), "png");
        return "org/%s/case/%s/analysis/%s/%s/%s/%s/%s/%s.%s".formatted(
                id(request.orgId(), "orgId"),
                segment(request.caseNo(), "caseNo"),
                segment(request.taskNo(), "taskNo"),
                segment(request.modelVersion(), "modelVersion"),
                upperSegment(request.assetTypeCode(), "assetTypeCode"),
                nullableId(request.relatedImageId()),
                optionalSegment(request.toothCode()),
                id(request.attachmentId(), "attachmentId"),
                ext);
    }

    private String reportKey(ObjectKeyRequest request) {
        return "org/%s/case/%s/report/%s/v%d/%s.pdf".formatted(
                id(request.orgId(), "orgId"),
                segment(request.caseNo(), "caseNo"),
                upperSegment(request.reportTypeCode(), "reportTypeCode"),
                requiredVersion(request.versionNo()),
                segment(request.reportNo(), "reportNo"));
    }

    private String exportKey(ObjectKeyRequest request) {
        LocalDate date = date(request);
        return "org/%s/export/%04d/%02d/%02d/%s/%s/%s.%s".formatted(
                id(request.orgId(), "orgId"),
                date.getYear(),
                date.getMonthValue(),
                date.getDayOfMonth(),
                id(request.operatorId(), "operatorId"),
                id(request.exportLogId(), "exportLogId"),
                segment(request.reportNo(), "reportNo"),
                extension(request.originalFileName(), "pdf"));
    }

    private LocalDate date(ObjectKeyRequest request) {
        return request.date() == null ? LocalDate.now() : request.date();
    }

    private String required(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String id(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return String.valueOf(value);
    }

    private int requiredVersion(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("versionNo is required");
        }
        return value;
    }

    private String nullableId(Long value) {
        return value == null ? "NA" : String.valueOf(value);
    }

    private String optionalSegment(String value) {
        return StringUtils.hasText(value) ? segment(value, "value") : "NA";
    }

    private String upper(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String upperSegment(String value, String fieldName) {
        return segment(upper(required(value, fieldName)), fieldName);
    }

    private String segment(String value, String fieldName) {
        String required = required(value, fieldName)
                .replace('\\', '/')
                .replaceAll("/", "-")
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-");
        String cleaned = trimEdgeDash(required);
        if (!StringUtils.hasText(cleaned)) {
            throw new IllegalArgumentException(fieldName + " is invalid");
        }
        return cleaned;
    }

    private String fileName(String originalFileName) {
        String candidate = StringUtils.hasText(originalFileName) ? originalFileName.trim() : "object.bin";
        candidate = candidate.replace('\\', '/');
        int slash = candidate.lastIndexOf('/');
        if (slash >= 0) {
            candidate = candidate.substring(slash + 1);
        }
        candidate = candidate.replaceAll("[^A-Za-z0-9._-]", "-").replaceAll("-+", "-");
        candidate = trimEdgeDash(candidate);
        return StringUtils.hasText(candidate) ? candidate : "object.bin";
    }

    private String extension(String originalFileName, String fallback) {
        String fileName = fileName(originalFileName);
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return fallback;
        }
        return segment(fileName.substring(dot + 1).toLowerCase(Locale.ROOT), "fileExt");
    }

    private String trimEdgeDash(String value) {
        String result = value;
        while (result.startsWith("-")) {
            result = result.substring(1);
        }
        while (result.endsWith("-")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
