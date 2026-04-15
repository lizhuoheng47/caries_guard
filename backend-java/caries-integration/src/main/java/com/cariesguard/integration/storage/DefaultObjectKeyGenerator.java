package com.cariesguard.integration.storage;

import java.time.LocalDate;
import java.util.Locale;
import org.springframework.util.StringUtils;

public class DefaultObjectKeyGenerator implements ObjectKeyGenerator {

    @Override
    public String generate(String bizModule, String bizId, String originalFileName, LocalDate date) {
        LocalDate resolvedDate = date == null ? LocalDate.now() : date;
        return "%s/%04d/%02d/%02d/%s/%s".formatted(
                segment(bizModule, "misc"),
                resolvedDate.getYear(),
                resolvedDate.getMonthValue(),
                resolvedDate.getDayOfMonth(),
                segment(bizId, "unknown"),
                fileName(originalFileName));
    }

    private String segment(String value, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String cleaned = value.trim()
                .replace('\\', '/')
                .replaceAll("/", "-")
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-+", "-");
        cleaned = trimEdgeDash(cleaned);
        return StringUtils.hasText(cleaned) ? cleaned.toLowerCase(Locale.ROOT) : fallback;
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
