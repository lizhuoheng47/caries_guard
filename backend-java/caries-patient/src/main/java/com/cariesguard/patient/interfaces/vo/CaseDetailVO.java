package com.cariesguard.patient.interfaces.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record CaseDetailVO(
        Long caseId,
        String caseNo,
        Long patientId,
        Long visitId,
        String caseStatusCode,
        String reportReadyFlag,
        String followupRequiredFlag,
        List<CaseImageVO> images,
        List<CaseDiagnosisVO> diagnoses,
        JsonNode latestAiSummary) {
}
