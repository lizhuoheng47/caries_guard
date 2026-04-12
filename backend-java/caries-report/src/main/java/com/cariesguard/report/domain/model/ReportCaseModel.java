package com.cariesguard.report.domain.model;

public record ReportCaseModel(
        Long caseId,
        String caseNo,
        Long patientId,
        String caseStatusCode,
        Long orgId) {
}

