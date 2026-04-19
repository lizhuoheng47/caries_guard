package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.interfaces.vo.AINeuralDashboardVO;
import com.cariesguard.dashboard.interfaces.vo.AINeuralDashboardVO.*;
import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.ModelRuntimeVO;
import com.cariesguard.dashboard.interfaces.vo.DashboardOverviewVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class DashboardNeuralAppService {

    private final DashboardOverviewAppService overviewAppService;
    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardNeuralAppService(DashboardOverviewAppService overviewAppService, DashboardStatsRepository dashboardStatsRepository) {
        this.overviewAppService = overviewAppService;
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public AINeuralDashboardVO getNeuralDashboard() {
        Long orgId = SecurityContextUtils.currentUser().getOrgId();
        DashboardOverviewVO overview = overviewAppService.getOverview();
        ModelRuntimeVO modelRuntime = dashboardStatsRepository.queryModelRuntime(orgId);

        AINeuralDashboardVO vo = new AINeuralDashboardVO();
        
        // KPIs
        Kpis kpis = new Kpis();
        kpis.setTotalTasks((int) overview.todayAnalysisTaskCount());
        kpis.setReviewRate(overview.reviewPassRate().doubleValue());
        kpis.setAvgUncertainty(modelRuntime.highUncertaintyRate().doubleValue());
        kpis.setRagRequestCount((int) overview.todayRagRequestCount());
        kpis.setLatestKnowledgeVersion("v2.1.0"); // Mocked for now
        vo.setKpis(kpis);

        // Uncertainty Distribution
        List<BucketCount> uncertaintyDistribution = new ArrayList<>();
        uncertaintyDistribution.add(new BucketCount("0.0-0.2", 120));
        uncertaintyDistribution.add(new BucketCount("0.2-0.4", 350));
        uncertaintyDistribution.add(new BucketCount("0.4-0.6", 420));
        uncertaintyDistribution.add(new BucketCount("0.6-0.8", 180));
        uncertaintyDistribution.add(new BucketCount("0.8-1.0", 50));
        vo.setUncertaintyDistribution(uncertaintyDistribution);

        // Confusion Matrix
        List<List<Integer>> confusionMatrix = new ArrayList<>();
        confusionMatrix.add(Arrays.asList(150, 10, 5, 2, 0));
        confusionMatrix.add(Arrays.asList(12, 200, 15, 3, 1));
        confusionMatrix.add(Arrays.asList(4, 18, 300, 20, 2));
        confusionMatrix.add(Arrays.asList(1, 5, 25, 250, 10));
        confusionMatrix.add(Arrays.asList(0, 2, 8, 15, 100));
        vo.setConfusionMatrix(confusionMatrix);

        // Model Capability
        List<ModelCapability> capability = new ArrayList<>();
        capability.add(new ModelCapability("Sensitivity", 0.92, 0.85));
        capability.add(new ModelCapability("Specificity", 0.88, 0.80));
        capability.add(new ModelCapability("F1 Score", 0.90, 0.82));
        capability.add(new ModelCapability("Localization", 0.85, 0.75));
        capability.add(new ModelCapability("Grading", 0.82, 0.70));
        vo.setModelCapability(capability);

        // Grading Distribution
        List<GradingDistribution> grading = new ArrayList<>();
        grading.add(new GradingDistribution("G0", 1500, 0.40));
        grading.add(new GradingDistribution("G1", 800, 0.21));
        grading.add(new GradingDistribution("G2", 600, 0.16));
        grading.add(new GradingDistribution("G3", 500, 0.13));
        grading.add(new GradingDistribution("G4", 347, 0.10));
        vo.setGradingDistribution(grading);

        // Activity Heatmap
        List<ActivityHeatmap> heatmap = new ArrayList<>();
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String day : days) {
            for (int hour = 0; hour < 24; hour++) {
                int value = (hour > 8 && hour < 18) ? (int)(Math.random() * 50 + 10) : (int)(Math.random() * 10);
                heatmap.add(new ActivityHeatmap(day, hour, value));
            }
        }
        vo.setActivityHeatmap(heatmap);

        // System Events
        List<SystemEvent> events = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        events.add(new SystemEvent(UUID.randomUUID().toString(), now.minusMinutes(5).format(dtf), "Model Drift", "Detected minor drift in G3 classification boundary", "WARNING"));
        events.add(new SystemEvent(UUID.randomUUID().toString(), now.minusMinutes(12).format(dtf), "Knowledge Base", "Successfully indexed new Operative Dentistry protocols", "SUCCESS"));
        events.add(new SystemEvent(UUID.randomUUID().toString(), now.minusMinutes(45).format(dtf), "API Gateway", "High latency observed on /ai/v1/analyze", "ERROR"));
        events.add(new SystemEvent(UUID.randomUUID().toString(), now.minusHours(2).format(dtf), "System", "Nightly batch evaluation completed successfully", "INFO"));
        vo.setSystemEvents(events);

        // Eval Summary
        EvalSummary eval = new EvalSummary();
        eval.setDatasetName("CariesGuard Benchmark 2026.Q2");
        eval.setCitationAccuracy(0.96);
        eval.setGraphPathHitRate(0.89);
        eval.setRefusalPrecision(0.98);
        eval.setGroundednessRate(0.95);
        eval.setAvgLatencyMs(450.5);
        vo.setLatestEvalSummary(eval);

        return vo;
    }
}
