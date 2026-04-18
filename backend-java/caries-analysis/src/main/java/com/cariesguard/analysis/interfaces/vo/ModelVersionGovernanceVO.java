package com.cariesguard.analysis.interfaces.vo;

import java.time.LocalDateTime;

public record ModelVersionGovernanceVO(
        Long modelVersionId,
        String modelCode,
        String modelVersion,
        String modelTypeCode,
        String approvedFlag,
        Long approvedBy,
        LocalDateTime approvedAt,
        String status,
        String remark) {
}
