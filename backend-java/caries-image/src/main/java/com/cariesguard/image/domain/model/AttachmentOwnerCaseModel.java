package com.cariesguard.image.domain.model;

public record AttachmentOwnerCaseModel(
        Long caseId,
        String caseNo,
        Long visitId,
        Long patientId,
        Long orgId,
        String caseStatusCode) {
}
