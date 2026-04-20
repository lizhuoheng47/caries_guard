package com.cariesguard.analysis.interfaces.vo;

import java.util.List;
import lombok.Data;

@Data
public class ReviewTaskDetailVO {
    private AnalysisTaskDetailVO task;
    private AnalysisDetailViewVO.CaseBriefVO caseInfo;
    private AnalysisDetailViewVO.ImageDetailVO image;
    private AiResultVO aiResult;
    private DoctorDraftVO doctorDraft;
    private SecondOpinionVO secondOpinion;
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
        private String doctorConfirmedGrade;
        private Boolean agreedWithAi;
        private String correctionReasonCategoryCode;
        private String reasonNote;
    }

    @Data
    public static class SecondOpinionVO {
        private String secondOpinionNo;
        private String statusCode;
        private String requestedBy;
        private String requestedAt;
        private String comment;
    }

    @Data
    public static class ReviewOptionsVO {
        private List<String> gradeOptions;
        private List<String> reasonTags;
    }
}
