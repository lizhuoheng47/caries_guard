package com.cariesguard.system.interfaces.vo;

public record DictItemVO(
        String label,
        String value,
        String code,
        int sortOrder,
        boolean defaultFlag) {
}
