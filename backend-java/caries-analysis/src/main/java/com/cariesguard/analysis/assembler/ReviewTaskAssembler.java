package com.cariesguard.analysis.assembler;

import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.interfaces.vo.AnalysisDetailViewVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisSummaryVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.ReviewTaskDetailVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.cariesguard.image.app.AttachmentAppService;
import com.cariesguard.image.interfaces.vo.AttachmentAccessVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReviewTaskAssembler {

    private static final List<String> GRADE_OPTIONS = List.of("G0", "G1", "G2", "G3", "G4");
    private static final List<String> REASON_TAG_OPTIONS = List.of(
            "HIGH_UNCERTAINTY",
            "LOW_CONFIDENCE",
            "BOUNDARY_CASE",
            "MULTI_LESION",
            "QUALITY_ISSUE",
            "MANUAL_REQUEST",
            "OTHER");

    private final AttachmentAppService attachmentAppService;

    public ReviewTaskAssembler(AttachmentAppService attachmentAppService) {
        this.attachmentAppService = attachmentAppService;
    }

    public ReviewTaskDetailVO toDetail(AnalysisTaskDetailVO task,
                                       AnalysisCaseModel medicalCase,
                                       AnalysisImageModel imageModel) {
        ReviewTaskDetailVO vo = new ReviewTaskDetailVO();
        AnalysisSummaryVO summary = task.summary();
        vo.setTask(task);
        vo.setReviewStatusCode(resolveNeedsReview(summary) ? "REVIEW_PENDING" : "NOT_REQUIRED");
        vo.setCaseInfo(toCaseInfo(task, medicalCase));
        vo.setImage(toImage(imageModel));
        vo.setAiResult(toAiResult(task));
        vo.setDoctorDraft(new ReviewTaskDetailVO.DoctorDraftVO());
        vo.setSecondOpinion(new ReviewTaskDetailVO.SecondOpinionVO());
        vo.setReviewOptions(defaultReviewOptions());
        return vo;
    }

    private AnalysisDetailViewVO.CaseBriefVO toCaseInfo(AnalysisTaskDetailVO task, AnalysisCaseModel medicalCase) {
        AnalysisDetailViewVO.CaseBriefVO vo = new AnalysisDetailViewVO.CaseBriefVO();
        vo.setCaseId(task.caseId());
        if (medicalCase != null && StringUtils.hasText(medicalCase.caseNo())) {
            vo.setCaseNo(medicalCase.caseNo());
        }
        return vo;
    }

    private AnalysisDetailViewVO.ImageDetailVO toImage(AnalysisImageModel imageModel) {
        AnalysisDetailViewVO.ImageDetailVO vo = new AnalysisDetailViewVO.ImageDetailVO();
        if (imageModel == null) {
            return vo;
        }
        vo.setImageId(imageModel.imageId());
        vo.setSourceDevice(imageModel.imageTypeCode());
        vo.setImageUrl(resolveImageUrl(imageModel.attachmentId()));
        return vo;
    }

    private ReviewTaskDetailVO.AiResultVO toAiResult(AnalysisTaskDetailVO task) {
        ReviewTaskDetailVO.AiResultVO vo = new ReviewTaskDetailVO.AiResultVO();
        AnalysisSummaryVO summary = task.summary();
        if (summary == null) {
            vo.setVisualAssets(task.visualAssets());
            vo.setDetections(List.of());
            return vo;
        }
        vo.setGradingLabel(summary.gradingLabel());
        vo.setConfidenceScore(summary.confidenceScore());
        vo.setUncertaintyScore(summary.uncertaintyScore());
        vo.setNeedsReview(resolveNeedsReview(summary));
        vo.setRiskLevelCode(summary.riskLevel());
        vo.setRiskLevelLabel(summary.riskLevelLabel());
        vo.setVisualAssets(task.visualAssets());
        vo.setDetections(extractDetections(summary));
        return vo;
    }

    private ReviewTaskDetailVO.ReviewOptionsVO defaultReviewOptions() {
        ReviewTaskDetailVO.ReviewOptionsVO vo = new ReviewTaskDetailVO.ReviewOptionsVO();
        vo.setGradeOptions(GRADE_OPTIONS);
        vo.setReasonTags(REASON_TAG_OPTIONS);
        return vo;
    }

    private Boolean resolveNeedsReview(AnalysisSummaryVO summary) {
        if (summary.needsReview() != null) {
            return summary.needsReview();
        }
        return "1".equals(summary.reviewSuggestedFlag());
    }

    private String resolveImageUrl(Long attachmentId) {
        if (attachmentId == null) {
            return null;
        }
        try {
            AttachmentAccessVO access = attachmentAppService.createInternalAccessUrl(attachmentId);
            return access.accessUrl();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private List<ReviewTaskDetailVO.DetectionBoxVO> extractDetections(AnalysisSummaryVO summary) {
        if (summary == null || summary.rawResultJson() == null) {
            return List.of();
        }
        JsonNode root = summary.rawResultJson();
        JsonNode lesionResults = root.path("lesionResults");
        double imageWidth = numberValue(root, "annotationImageWidth");
        double imageHeight = numberValue(root, "annotationImageHeight");
        if (!lesionResults.isArray() || lesionResults.isEmpty() || imageWidth <= 0 || imageHeight <= 0) {
            return List.of();
        }

        List<ReviewTaskDetailVO.DetectionBoxVO> detections = new ArrayList<>();
        int index = 1;
        for (JsonNode lesion : lesionResults) {
            JsonNode bbox = lesion.path("bbox");
            if (!bbox.isArray() || bbox.size() != 4) {
                continue;
            }
            double x1 = bbox.path(0).asDouble(-1);
            double y1 = bbox.path(1).asDouble(-1);
            double x2 = bbox.path(2).asDouble(-1);
            double y2 = bbox.path(3).asDouble(-1);
            if (x1 < 0 || y1 < 0 || x2 <= x1 || y2 <= y1) {
                continue;
            }
            ReviewTaskDetailVO.DetectionBoxVO detection = new ReviewTaskDetailVO.DetectionBoxVO();
            detection.setId("lesion-" + index);
            detection.setX(roundRatio(x1 / imageWidth));
            detection.setY(roundRatio(y1 / imageHeight));
            detection.setWidth(roundRatio((x2 - x1) / imageWidth));
            detection.setHeight(roundRatio((y2 - y1) / imageHeight));
            detection.setLabel(textValue(lesion, "severityCode"));
            if (lesion.hasNonNull("confidenceScore")) {
                detection.setConfidence(lesion.path("confidenceScore").asDouble());
            }
            detections.add(detection);
            index++;
        }
        return detections;
    }

    private String textValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text : null;
    }

    private double numberValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? value.asDouble() : -1;
    }

    private double roundRatio(double value) {
        return Math.max(0, Math.min(1, Math.round(value * 10000d) / 10000d));
    }
}
