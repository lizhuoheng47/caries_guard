package com.cariesguard.analysis.domain.model;

public record AnalysisImageModel(
        Long imageId,
        Long caseId,
        Long attachmentId,
        String imageTypeCode,
        String qualityStatusCode,
        String bucketName,
        String objectKey) {
}
