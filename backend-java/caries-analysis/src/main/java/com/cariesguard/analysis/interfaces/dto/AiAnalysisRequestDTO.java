package com.cariesguard.analysis.interfaces.dto;

import java.util.List;

public record AiAnalysisRequestDTO(
        String taskNo,
        String taskTypeCode,
        Long caseId,
        Long patientId,
        Long orgId,
        String modelVersion,
        List<ImageItem> images,
        PatientProfile patientProfile) {

    public record ImageItem(
            Long imageId,
            Long attachmentId,
            String imageTypeCode,
            String bucketName,
            String objectKey) {
    }

    public record PatientProfile(
            Integer age,
            String genderCode) {
    }
}
