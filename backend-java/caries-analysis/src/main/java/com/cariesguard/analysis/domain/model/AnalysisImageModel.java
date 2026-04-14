package com.cariesguard.analysis.domain.model;

public record AnalysisImageModel(
        Long imageId,
        Long caseId,
        Long attachmentId,
        String imageTypeCode,
        String qualityStatusCode,
        String bucketName,
        String objectKey,
        String storageProviderCode,
        String attachmentMd5) {

    public AnalysisImageModel(Long imageId,
                              Long caseId,
                              Long attachmentId,
                              String imageTypeCode,
                              String qualityStatusCode,
                              String bucketName,
                              String objectKey) {
        this(imageId, caseId, attachmentId, imageTypeCode, qualityStatusCode, bucketName, objectKey, null, null);
    }
}
