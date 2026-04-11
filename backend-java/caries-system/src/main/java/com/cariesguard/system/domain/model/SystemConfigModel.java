package com.cariesguard.system.domain.model;

public record SystemConfigModel(
        String configKey,
        String configName,
        String configValue,
        String valueTypeCode,
        boolean sensitive) {
}
