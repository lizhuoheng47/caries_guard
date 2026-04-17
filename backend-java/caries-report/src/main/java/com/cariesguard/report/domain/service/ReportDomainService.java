package com.cariesguard.report.domain.service;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.report.domain.model.ReportRenderDataModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReportDomainService {

    private static final Set<String> REPORT_TYPES = Set.of("DOCTOR", "PATIENT");
    private static final Set<String> GENERATE_ALLOWED_CASE_STATUS = Set.of("REVIEW_PENDING", "REPORT_READY");
    private static final DateTimeFormatter REPORT_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String normalizeReportType(String reportTypeCode) {
        if (!StringUtils.hasText(reportTypeCode)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Report type is required");
        }
        String normalized = reportTypeCode.trim().toUpperCase();
        if (!REPORT_TYPES.contains(normalized)) {
            throw new BusinessException(CommonErrorCode.VALIDATION_FAILED.code(), "Unsupported report type");
        }
        return normalized;
    }

    public void ensureCaseStatusAllowed(String caseStatusCode) {
        if (!GENERATE_ALLOWED_CASE_STATUS.contains(caseStatusCode)) {
            throw new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case status does not allow report generation");
        }
    }

    public String buildReportNo(long reportId) {
        return "RPT" + LocalDate.now().format(REPORT_NO_DATE_FORMATTER) + String.format("%06d", reportId % 1_000_000);
    }

    public String draftStatus() {
        return "DRAFT";
    }

    public String finalStatus() {
        return "FINAL";
    }

    public String normalizeExportType(String exportTypeCode) {
        if (!StringUtils.hasText(exportTypeCode)) {
            return "PDF";
        }
        return exportTypeCode.trim().toUpperCase();
    }

    public String normalizeExportChannel(String exportChannelCode) {
        if (!StringUtils.hasText(exportChannelCode)) {
            return "DOWNLOAD";
        }
        return exportChannelCode.trim().toUpperCase();
    }

    public String buildSummaryText(ReportRenderDataModel renderData) {
        String severity = StringUtils.hasText(renderData.highestSeverity()) ? renderData.highestSeverity() : "UNKNOWN";
        String risk = StringUtils.hasText(renderData.riskLevelCode()) ? renderData.riskLevelCode() : "UNKNOWN";
        return "case=" + renderData.caseNo()
                + ", type=" + renderData.reportTypeCode()
                + ", severity=" + severity
                + ", risk=" + risk
                + ", images=" + renderData.imageCount()
                + ", teeth=" + renderData.toothRecordCount()
                + ", visuals=" + renderData.visualAssetCount()
                + ", corrections=" + renderData.correctionCount();
    }
}
