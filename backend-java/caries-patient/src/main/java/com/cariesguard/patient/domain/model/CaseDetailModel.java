package com.cariesguard.patient.domain.model;

import java.util.List;

public record CaseDetailModel(
        Long caseId,
        String caseNo,
        Long patientId,
        Long visitId,
        String caseStatusCode,
        String reportReadyFlag,
        String followupRequiredFlag,
        Long orgId,
        List<CaseImageModel> images,
        List<CaseDiagnosisModel> diagnoses,
        String latestAiSummaryRaw) {
}
