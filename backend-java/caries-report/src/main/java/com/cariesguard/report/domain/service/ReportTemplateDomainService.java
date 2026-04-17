package com.cariesguard.report.domain.service;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.report.domain.model.ReportTemplateModel;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportTemplateDomainService {

    public String resolveTemplateContent(String reportTypeCode, Optional<ReportTemplateModel> template) {
        return template.map(ReportTemplateModel::templateContent)
                .filter(StringUtils::hasText)
                .orElseGet(() -> defaultTemplate(reportTypeCode));
    }

    public void validateTemplateContent(String templateContent) {
        if (!StringUtils.hasText(templateContent)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Template content is required");
        }
    }

    private String defaultTemplate(String reportTypeCode) {
        if ("PATIENT".equals(reportTypeCode)) {
            return """
                    Report No: {{reportNo}}
                    Case No: {{caseNo}}
                    Risk Level: {{riskLevelCode}}
                    Possible Findings: {{highestSeverity}}
                    Summary: {{clinicalSummary}}
                    Patient Explanation: {{patientExplanation}}
                    Recheck Suggestion (days): {{recommendedCycleDays}}
                    Care Suggestion: {{patientAdvice}}
                    """;
        }
        return """
                Report No: {{reportNo}}
                Case No: {{caseNo}}
                Severity: {{highestSeverity}}
                Uncertainty: {{uncertaintyScore}}
                Lesion Count: {{lesionCount}}
                Abnormal Tooth Count: {{abnormalToothCount}}
                Risk Level: {{riskLevelCode}}
                Suggested Cycle (days): {{recommendedCycleDays}}
                Review Suggested: {{reviewSuggestedFlag}}
                Images:
                {{images}}
                Tooth Findings:
                {{toothFindings}}
                Visual Assets:
                {{visualAssets}}
                Corrections:
                {{corrections}}
                Doctor Conclusion: {{doctorConclusion}}
                """;
    }
}
