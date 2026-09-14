package com.cariesguard.analysis.interfaces.vo;

import java.util.List;
import lombok.Data;

@Data
public class ReviewTaskDetailVO {
    private AnalysisTaskDetailVO task;
    private String reviewStatusCode;
    private AnalysisDetailViewVO.CaseBriefVO caseInfo;
    private AnalysisDetailViewVO.ImageDetailVO image;
    private AiResultVO aiResult;
    private DoctorDraftVO doctorDraft;
    private ReviewOptionsVO reviewOptions;

    @Data
    public static class AiResultVO {
        private String gradingLabel;
        private Double confidenceScore;
        private Double uncertaintyScore;
        private Boolean needsReview;
        private String riskLevelCode;
        private String riskLevelLabel;
        private List<DetectionBoxVO> detections;
        private List<AnalysisVisualAssetVO> visualAssets;
    }

    @Data
    public static class DetectionBoxVO {
        private String id;
        private Double x;
        private Double y;
        private Double width;
        private Double height;
        private String label;
        private Double confidence;
    }

    @Data
    public static class DoctorDraftVO {
        private Long draftId;
        private Integer draftVersion;
        private String revisedGrade;
        private List<DetectionBoxVO> revisedDetections;
        private List<String> reasonTags;
        private String note;
        private String statusCode;
    }

    @Data
    @Data
    public static class ReviewOptionsVO {
        private List<String> gradeOptions;
        private List<String> reasonTags;
    }
}
