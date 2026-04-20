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
        private List<ReviewWorkbenchVO.DetectionBoxVO> detections;
        private List<AnalysisVisualAssetVO> visualAssets;
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

