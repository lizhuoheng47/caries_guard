package com.cariesguard.system.interfaces.vo;

public record DictTypeVO(
        String dictType,
        String dictName,
        boolean systemFlag,
        int sortOrder) {
}
