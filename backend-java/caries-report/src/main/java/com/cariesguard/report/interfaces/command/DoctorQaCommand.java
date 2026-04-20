package com.cariesguard.report.interfaces.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record DoctorQaCommand(
        @NotBlank String question,
        String kbCode,
        @Min(1) @Max(20) Integer topK,
        String relatedBizNo,
        String patientUuid,
        Map<String, Object> clinicalContext,
        String taskNo) {

    public DoctorQaCommand(String question,
                           String kbCode,
                           Integer topK,
                           String relatedBizNo,
                           String patientUuid,
                           Map<String, Object> clinicalContext) {
        this(question, kbCode, topK, relatedBizNo, patientUuid, clinicalContext, null);
    }
}
