package com.cariesguard.followup.domain.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FollowupDomainService {

    private static final DateTimeFormatter NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final int DEFAULT_INTERVAL_DAYS = 30;
    private static final int HIGH_RISK_INTERVAL_DAYS = 14;

    public boolean shouldTriggerFollowup(String riskLevelCode, String reviewSuggestedFlag) {
        return "HIGH".equals(riskLevelCode) || "1".equals(reviewSuggestedFlag);
    }

    public String resolveTriggerSource(String riskLevelCode, String reviewSuggestedFlag) {
        if ("HIGH".equals(riskLevelCode)) {
            return "RISK_HIGH";
        }
        if ("1".equals(reviewSuggestedFlag)) {
            return "REPORT_REVIEW";
        }
        return "DOCTOR_MANUAL";
    }

    public String resolvePlanType(String triggerSourceCode) {
        return "RISK_HIGH".equals(triggerSourceCode) ? "HIGH_RISK" : "ROUTINE";
    }

    public int resolveIntervalDays(String riskLevelCode, Integer recommendedCycleDays) {
        if (recommendedCycleDays != null && recommendedCycleDays > 0) {
            return recommendedCycleDays;
        }
        return "HIGH".equals(riskLevelCode) ? HIGH_RISK_INTERVAL_DAYS : DEFAULT_INTERVAL_DAYS;
    }

    public LocalDate resolveFirstTaskDueDate(int intervalDays) {
        return LocalDate.now().plusDays(intervalDays);
    }

    public String buildPlanNo(long planId) {
        return "FUP" + LocalDate.now().format(NO_DATE_FORMATTER) + String.format("%06d", planId % 1_000_000);
    }

    public String buildTaskNo(long taskId) {
        return "TSK" + LocalDate.now().format(NO_DATE_FORMATTER) + String.format("%06d", taskId % 1_000_000);
    }

    public String buildRecordNo(long recordId) {
        return "REC" + LocalDate.now().format(NO_DATE_FORMATTER) + String.format("%06d", recordId % 1_000_000);
    }

    public String normalizeFollowupMethod(String code) {
        if (!StringUtils.hasText(code)) {
            return "PHONE";
        }
        return code.trim().toUpperCase();
    }

    public String normalizeContactResult(String code) {
        if (!StringUtils.hasText(code)) {
            return "REACHED";
        }
        return code.trim().toUpperCase();
    }
}
