package com.cariesguard.image.domain.model;

public record ImageManagedModel(
        Long imageId,
        Long caseId,
        Long patientId,
        Long attachmentId,
        Long orgId) {
}
