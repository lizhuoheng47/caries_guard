package com.cariesguard.analysis.domain.service;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Domain service for correction feedback rules.
 * Validates that the case is in an appropriate state for doctor corrections.
 */
@Component
public class CorrectionFeedbackDomainService {

    private static final Set<String> ALLOWED_CASE_STATUSES = Set.of("REVIEW_PENDING");

    public void ensureCaseAllowsCorrection(String caseStatusCode) {
        if (!ALLOWED_CASE_STATUSES.contains(caseStatusCode)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(),
                    "Case does not allow correction feedback in status: " + caseStatusCode);
        }
    }

    public void validateFeedbackTypeCode(String feedbackTypeCode) {
        if (!StringUtils.hasText(feedbackTypeCode)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(),
                    "feedbackTypeCode is required for correction feedback");
        }
    }
}
