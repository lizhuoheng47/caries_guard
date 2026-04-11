package com.cariesguard.system.domain.model;

public record DictTypeModel(
        String dictType,
        String dictName,
        boolean systemFlag,
        int sortOrder) {
}
