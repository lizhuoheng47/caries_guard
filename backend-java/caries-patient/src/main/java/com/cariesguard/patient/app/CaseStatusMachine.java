package com.cariesguard.patient.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CaseStatusMachine {

    private static final Set<String> TERMINAL_STATUSES = Set.of("CLOSED", "CANCELLED");
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "CREATED", Set.of("QC_PENDING", "CANCELLED"),
            "QC_PENDING", Set.of("ANALYZING", "CREATED", "CANCELLED"),
            "ANALYZING", Set.of("REVIEW_PENDING", "QC_PENDING", "CANCELLED"),
            "REVIEW_PENDING", Set.of("REPORT_READY", "FOLLOWUP_REQUIRED", "CANCELLED"),
            "REPORT_READY", Set.of("FOLLOWUP_REQUIRED", "CLOSED"),
            "FOLLOWUP_REQUIRED", Set.of("CLOSED", "CANCELLED"));

    public void ensureTransitionAllowed(String currentStatus, String targetStatus) {
        if (!StringUtils.hasText(targetStatus)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Target case status is required");
        }
        String normalizedTarget = targetStatus.trim();
        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Terminal case status cannot transition");
        }
        if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(normalizedTarget)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case transition is not allowed");
        }
    }
}
