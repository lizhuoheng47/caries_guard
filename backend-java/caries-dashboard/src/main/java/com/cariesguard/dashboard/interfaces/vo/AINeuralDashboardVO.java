package com.cariesguard.dashboard.interfaces.vo;

import lombok.Data;
import java.util.List;

@Data
public class AINeuralDashboardVO {
    private Kpis kpis;
    private List<BucketCount> uncertaintyDistribution;
    private List<List<Integer>> confusionMatrix;
    private List<ModelCapability> modelCapability;
    private List<GradingDistribution> gradingDistribution;
    private List<ActivityHeatmap> activityHeatmap;
    private List<SystemEvent> systemEvents;
    private EvalSummary latestEvalSummary;

    @Data
    public static class Kpis {
        private Integer totalTasks;
        private Double reviewRate;
        private Double avgUncertainty;
    }

    @Data
    public static class BucketCount {
        private String bucket;
        private Integer count;

        public BucketCount(String bucket, Integer count) {
            this.bucket = bucket;
            this.count = count;
        }
    }

    @Data
    public static class ModelCapability {
        private String dimension;
        private Double score;
        private Double baseline;

        public ModelCapability(String dimension, Double score, Double baseline) {
            this.dimension = dimension;
            this.score = score;
            this.baseline = baseline;
        }
    }

    @Data
    public static class GradingDistribution {
        private String grade;
        private Integer count;
        private Double ratio;

        public GradingDistribution(String grade, Integer count, Double ratio) {
            this.grade = grade;
            this.count = count;
            this.ratio = ratio;
        }
    }

    @Data
    public static class ActivityHeatmap {
        private String day;
        private Integer hour;
        private Integer value;

        public ActivityHeatmap(String day, Integer hour, Integer value) {
            this.day = day;
            this.hour = hour;
            this.value = value;
        }
    }

    @Data
    public static class SystemEvent {
        private String id;
        private String time;
        private String type;
        private String message;
        private String status;

        public SystemEvent(String id, String time, String type, String message, String status) {
            this.id = id;
            this.time = time;
            this.type = type;
            this.message = message;
            this.status = status;
        }
    }

    @Data
    public static class EvalSummary {
        private String datasetName;
        private Double citationAccuracy;
        private Double graphPathHitRate;
        private Double refusalPrecision;
        private Double groundednessRate;
        private Double avgLatencyMs;
    }
}
