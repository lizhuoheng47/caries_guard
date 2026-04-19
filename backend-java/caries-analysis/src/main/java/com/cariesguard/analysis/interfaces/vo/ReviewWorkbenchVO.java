package com.cariesguard.analysis.interfaces.vo;

import lombok.Data;
import java.util.List;

@Data
public class ReviewWorkbenchVO {
    private AnalysisDetailViewVO.AnalysisTaskDetailVO task;
    private AnalysisDetailViewVO.CaseBriefVO caseInfo;
    private AnalysisDetailViewVO.ImageDetailVO image;
    private AiResult aiResult;
    private DoctorDraft doctorDraft;
    private ReviewOptions reviewOptions;

    @Data
    public static class AiResult {
        private String gradingLabel;
        private Double uncertaintyScore;
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
    public static class DoctorDraft {
        private Long draftId;
        private String revisedGrade;
        private List<DetectionBoxVO> revisedDetections;
        private List<String> reasonTags;
        private String note;
    }

    @Data
    public static class ReviewOptions {
        private List<String> gradeOptions;
        private List<String> reasonTags;
    }
}
