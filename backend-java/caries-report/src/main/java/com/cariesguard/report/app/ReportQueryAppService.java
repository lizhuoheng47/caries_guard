package com.cariesguard.report.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import com.cariesguard.report.domain.model.ReportToothRecordModel;
import com.cariesguard.report.domain.model.ReportVisualAssetModel;
import com.cariesguard.report.domain.repository.ReportRecordRepository;
import com.cariesguard.report.domain.repository.ReportSourceQueryRepository;
import com.cariesguard.report.interfaces.vo.ReportAnalysisSummaryVO;
import com.cariesguard.report.interfaces.vo.ReportCorrectionVO;
import com.cariesguard.report.interfaces.vo.ReportDetailVO;
import com.cariesguard.report.interfaces.vo.ReportImageVO;
import com.cariesguard.report.interfaces.vo.ReportListItemVO;
import com.cariesguard.report.interfaces.vo.ReportRiskFactorVO;
import com.cariesguard.report.interfaces.vo.ReportRiskAssessmentVO;
import com.cariesguard.report.interfaces.vo.ReportToothRecordVO;
import com.cariesguard.report.interfaces.vo.ReportVisualAssetVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ReportQueryAppService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ReportRecordRepository reportRecordRepository;
    private final ReportSourceQueryRepository reportSourceQueryRepository;

    public ReportQueryAppService(ReportRecordRepository reportRecordRepository,
                                 ReportSourceQueryRepository reportSourceQueryRepository) {
        this.reportRecordRepository = reportRecordRepository;
        this.reportSourceQueryRepository = reportSourceQueryRepository;
    }

    public List<ReportListItemVO> listCaseReports(Long caseId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ReportCaseModel medicalCase = reportSourceQueryRepository.findCase(caseId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        ensureOrgAccess(operator, medicalCase.orgId());
        return reportRecordRepository.listByCaseId(caseId).stream()
                .map(this::toListItemVO)
                .toList();
    }

    public ReportDetailVO getReport(Long reportId) {
        AuthenticatedUser operator = SecurityContextUtils.currentUser();
        ReportRecordModel report = reportRecordRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Report does not exist"));
        ensureOrgAccess(operator, report.orgId());
        ReportCaseModel medicalCase = reportSourceQueryRepository.findCase(report.caseId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BUSINESS_ERROR.code(), "Case does not exist"));
        Optional<ReportAnalysisSummaryModel> summary = report.sourceSummaryId() == null
                ? reportSourceQueryRepository.findLatestSummary(report.caseId())
                : reportSourceQueryRepository.findSummaryById(report.sourceSummaryId());
        Optional<ReportRiskAssessmentModel> riskAssessment = report.sourceRiskAssessmentId() == null
                ? reportSourceQueryRepository.findLatestRiskAssessment(report.caseId())
                : reportSourceQueryRepository.findRiskAssessmentById(report.sourceRiskAssessmentId());
        List<ReportImageModel> images = reportSourceQueryRepository.listCaseImages(report.caseId());
        List<ReportToothRecordModel> toothRecords = reportSourceQueryRepository.listToothRecords(report.caseId());
        List<ReportVisualAssetModel> visualAssets = summary
                .map(item -> reportSourceQueryRepository.listVisualAssetsByTaskId(item.taskId()))
                .orElseGet(List::of);
        List<ReportCorrectionModel> corrections = reportSourceQueryRepository.listCorrections(report.caseId());
        return new ReportDetailVO(
                report.reportId(),
                report.reportNo(),
                report.caseId(),
                medicalCase.caseNo(),
                report.patientId(),
                report.attachmentId(),
                report.sourceSummaryId(),
                report.sourceRiskAssessmentId(),
                report.sourceCorrectionId(),
                report.reportTypeCode(),
                report.reportStatusCode(),
                report.versionNo(),
                report.summaryText(),
                summary.map(this::toSummaryVO).orElse(null),
                riskAssessment.map(this::toRiskAssessmentVO).orElse(null),
                images.stream().map(this::toImageVO).toList(),
                toothRecords.stream().map(this::toToothRecordVO).toList(),
                visualAssets.stream().map(this::toVisualAssetVO).toList(),
                corrections.stream().map(this::toCorrectionVO).toList(),
                report.generatedAt(),
                report.signedAt(),
                report.createdAt());
    }

    private ReportListItemVO toListItemVO(ReportRecordModel report) {
        return new ReportListItemVO(
                report.reportId(),
                report.reportNo(),
                report.reportTypeCode(),
                report.versionNo(),
                report.reportStatusCode(),
                report.attachmentId(),
                report.generatedAt(),
                report.createdAt());
    }

    private ReportAnalysisSummaryVO toSummaryVO(ReportAnalysisSummaryModel model) {
        return new ReportAnalysisSummaryVO(
                model.summaryId(),
                model.taskId(),
                model.overallHighestSeverity(),
                model.uncertaintyScore(),
                model.reviewSuggestedFlag(),
                model.lesionCount(),
                model.abnormalToothCount());
    }

    private ReportRiskAssessmentVO toRiskAssessmentVO(ReportRiskAssessmentModel model) {
        ParsedRiskAssessment parsed = parseRiskAssessment(model.assessmentReportJson());
        return new ReportRiskAssessmentVO(
                model.riskAssessmentId(),
                firstText(parsed.riskLevel(), model.overallRiskLevelCode()),
                firstDecimal(parsed.riskScore(), model.riskScore()),
                model.assessmentReportJson(),
                firstInt(parsed.recommendedCycleDays(), model.recommendedCycleDays()),
                parsed.followupSuggestion(),
                parsed.reviewSuggested(),
                parsed.explanation(),
                parsed.fusionVersion(),
                parsed.riskFactors().isEmpty()
                        ? model.riskFactors().stream().map(this::toRiskFactorVO).toList()
                        : parsed.riskFactors(),
                model.assessedAt());
    }

    private ReportRiskFactorVO toRiskFactorVO(ReportRiskAssessmentModel.RiskFactorModel model) {
        return new ReportRiskFactorVO(model.code(), model.weight(), model.source(), model.evidence());
    }

    private ReportImageVO toImageVO(ReportImageModel model) {
        return new ReportImageVO(
                model.imageId(),
                model.attachmentId(),
                model.imageTypeCode(),
                model.qualityStatusCode(),
                model.primaryFlag(),
                model.bucketName(),
                model.objectKey(),
                model.originalName());
    }

    private ReportToothRecordVO toToothRecordVO(ReportToothRecordModel model) {
        return new ReportToothRecordVO(
                model.toothRecordId(),
                model.sourceImageId(),
                model.toothCode(),
                model.toothSurfaceCode(),
                model.issueTypeCode(),
                model.severityCode(),
                model.findingDesc(),
                model.suggestion(),
                model.sortOrder());
    }

    private ReportVisualAssetVO toVisualAssetVO(ReportVisualAssetModel model) {
        return new ReportVisualAssetVO(
                model.visualAssetId(),
                model.taskId(),
                model.assetTypeCode(),
                model.attachmentId(),
                model.relatedImageId(),
                model.sourceAttachmentId(),
                model.toothCode(),
                model.sortOrder(),
                model.bucketName(),
                model.objectKey(),
                model.contentType(),
                model.originalName());
    }

    private ReportCorrectionVO toCorrectionVO(ReportCorrectionModel model) {
        return new ReportCorrectionVO(
                model.correctionId(),
                model.feedbackTypeCode(),
                model.correctedTruthJson(),
                model.createdAt());
    }

    private void ensureOrgAccess(AuthenticatedUser operator, Long recordOrgId) {
        if (!operator.hasAnyRole("ADMIN", "SYS_ADMIN") && !recordOrgId.equals(operator.getOrgId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
    }

    private ParsedRiskAssessment parseRiskAssessment(String assessmentReportJson) {
        if (assessmentReportJson == null || assessmentReportJson.isBlank()) {
            return ParsedRiskAssessment.empty();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(assessmentReportJson);
            JsonNode node = root.path("riskAssessment");
            if (node.isMissingNode() || node.isNull()) {
                node = root;
            }
            return new ParsedRiskAssessment(
                    text(node, "riskLevel", "riskLevelCode", "overallRiskLevelCode"),
                    decimal(node, "riskScore"),
                    integer(node, "recommendedCycleDays"),
                    text(node, "followupSuggestion"),
                    bool(node, "reviewSuggested"),
                    text(node, "explanation"),
                    text(node, "fusionVersion"),
                    riskFactors(node.path("riskFactors")));
        } catch (Exception ignored) {
            return ParsedRiskAssessment.empty();
        }
    }

    private List<ReportRiskFactorVO> riskFactors(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        java.util.ArrayList<ReportRiskFactorVO> factors = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            factors.add(new ReportRiskFactorVO(
                    text(item, "code", "featureCode"),
                    decimal(item, "weight", "contribution"),
                    text(item, "source"),
                    text(item, "evidence")));
        }
        return factors;
    }

    private static String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static BigDecimal firstDecimal(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private static Integer firstInt(Integer first, Integer second) {
        return first != null ? first : second;
    }

    private static String text(JsonNode node, String... fields) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static BigDecimal decimal(JsonNode node, String... fields) {
        String value = text(node, fields);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer integer(JsonNode node, String... fields) {
        String value = text(node, fields);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Boolean bool(JsonNode node, String... fields) {
        String value = text(node, fields);
        return value == null ? null : Boolean.valueOf(value);
    }

    private record ParsedRiskAssessment(
            String riskLevel,
            BigDecimal riskScore,
            Integer recommendedCycleDays,
            String followupSuggestion,
            Boolean reviewSuggested,
            String explanation,
            String fusionVersion,
            List<ReportRiskFactorVO> riskFactors) {

        private ParsedRiskAssessment {
            riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
        }

        private static ParsedRiskAssessment empty() {
            return new ParsedRiskAssessment(null, null, null, null, null, null, null, List.of());
        }
    }
}
