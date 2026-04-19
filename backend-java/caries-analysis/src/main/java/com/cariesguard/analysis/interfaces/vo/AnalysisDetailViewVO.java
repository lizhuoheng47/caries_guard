package com.cariesguard.analysis.interfaces.vo;

import lombok.Data;
import java.util.List;

@Data
public class AnalysisDetailViewVO {
    private AnalysisTaskDetailVO task;
    private PatientBriefVO patient;
    private CaseBriefVO caseInfo;
    private ImageDetailVO image;
    private AnalysisSummary analysisSummary;
    private List<TimelineNodeVO> timeline;
    private RagHint ragHint;

    @Data
    public static class PatientBriefVO {
        private String patientIdMasked;
        private String patientNameMasked;
        private Integer age;
        private String gender;
    }

    @Data
    public static class CaseBriefVO {
        private Long caseId;
        private String caseNo;
        private String visitTime;
    }

    @Data
    public static class ImageDetailVO {
        private Long imageId;
        private String imageUrl;
        private String sourceDevice;
    }

    @Data
    public static class AnalysisSummary {
        private String gradingLabel;
        private Double confidenceScore;
        private Double uncertaintyScore;
        private Boolean needsReview;
        private String riskLevel;
        private List<String> riskFactors;
        private List<AnalysisVisualAssetVO> visualAssets;
    }

    @Data
    public static class TimelineNodeVO {
        private String time;
        private String title;
        private String content;
        private String status;
        private String duration;
    }

    @Data
    public static class RagHint {
        private Boolean enabled;
        private String latestAnswer;
        private List<CitationVO> latestCitations;
    }

    @Data
    public static class CitationVO {
        private String docNo;
        private String title;
        private String content;
    }
}
