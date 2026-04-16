package com.cariesguard.integration.storage;

import java.time.LocalDate;

public record ObjectKeyRequest(
        String objectKindCode,
        Long orgId,
        String caseNo,
        String imageTypeCode,
        Long attachmentId,
        String taskNo,
        String modelVersion,
        String assetTypeCode,
        Long relatedImageId,
        String toothCode,
        String reportTypeCode,
        Integer versionNo,
        Long operatorId,
        Long exportLogId,
        String reportNo,
        String originalFileName,
        LocalDate date) {
}
