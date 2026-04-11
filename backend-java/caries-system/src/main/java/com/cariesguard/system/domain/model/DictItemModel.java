package com.cariesguard.system.domain.model;

public record DictItemModel(
        String label,
        String value,
        String code,
        int sortOrder,
        boolean defaultFlag) {
}
