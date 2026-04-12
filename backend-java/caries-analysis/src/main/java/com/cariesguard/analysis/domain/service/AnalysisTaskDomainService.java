package com.cariesguard.analysis.domain.service;

import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Domain service for analysis task creation rules.
 * All task creation business rules are centralized here — AppService only orchestrates.
 */
@Component
public class AnalysisTaskDomainService {

    private static final DateTimeFormatter TASK_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public void ensureCaseReadyForAnalysis(String caseStatusCode) {
        if (!"QC_PENDING".equals(caseStatusCode)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case is not ready for analysis, current status: " + caseStatusCode);
        }
    }

    public void ensureNoRunningTask(boolean existsRunning, boolean forceRetry) {
        if (!forceRetry && existsRunning) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case already has a running analysis task");
        }
    }

    public void ensureAnalyzableImagesExist(List<AnalysisImageModel> qualityPassedImages) {
        if (qualityPassedImages == null || qualityPassedImages.isEmpty()) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case has no quality-approved image");
        }
    }

    public void ensurePatientMatchesCase(Long commandPatientId, Long casePatientId) {
        if (commandPatientId != null && !commandPatientId.equals(casePatientId)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not belong to patient");
        }
    }

    public String generateTaskNo(long taskId) {
        return "TASK" + LocalDateTime.now().format(TASK_NO_FORMATTER) + String.format("%06d", taskId % 1_000_000);
    }

    public String resolveTaskTypeCode(String input) {
        return StringUtils.hasText(input) ? input.trim() : "INFERENCE";
    }

    public String resolveRetryReasonCode(String reasonCode) {
        return StringUtils.hasText(reasonCode) ? reasonCode.trim() : "AI_TASK_RETRY";
    }
}
