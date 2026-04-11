package com.cariesguard.system.interfaces.vo;

public record SystemConfigVO(
        String configKey,
        String configName,
        String configValue,
        String valueTypeCode,
        boolean sensitive) {
}
