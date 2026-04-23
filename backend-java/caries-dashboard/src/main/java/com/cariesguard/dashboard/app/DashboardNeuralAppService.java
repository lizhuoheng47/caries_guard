package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.AINeuralDashboardVO;
import com.cariesguard.dashboard.interfaces.vo.AINeuralDashboardVO.EvalSummary;
import com.cariesguard.dashboard.interfaces.vo.AINeuralDashboardVO.Kpis;
import com.cariesguard.dashboard.interfaces.vo.AINeuralDashboardVO.ModelCapability;
import com.cariesguard.dashboard.interfaces.vo.AINeuralDashboardVO.SystemEvent;
import com.cariesguard.dashboard.interfaces.vo.DashboardOverviewVO;
import com.cariesguard.dashboard.interfaces.vo.ModelRuntimeVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DashboardNeuralAppService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DashboardOverviewAppService overviewAppService;
    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardNeuralAppService(DashboardOverviewAppService overviewAppService,
                                     DashboardStatsRepository dashboardStatsRepository) {
        this.overviewAppService = overviewAppService;
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public AINeuralDashboardVO getNeuralDashboard() {
        Long orgId = SecurityContextUtils.currentUser().getOrgId();
        DashboardOverviewVO overview = overviewAppService.getOverview();
        ModelRuntimeVO modelRuntime = dashboardStatsRepository.queryModelRuntime(orgId);

        AINeuralDashboardVO vo = new AINeuralDashboardVO();
        vo.setKpis(buildKpis(overview, modelRuntime));
        vo.setUncertaintyDistribution(List.of());
        vo.setConfusionMatrix(List.of());
        vo.setModelCapability(buildModelCapability(modelRuntime));
        vo.setGradingDistribution(List.of());
        vo.setActivityHeatmap(List.of());
        vo.setSystemEvents(buildSystemEvents(modelRuntime));
        vo.setLatestEvalSummary(buildEvalSummary(modelRuntime));
        return vo;
    }

    private Kpis buildKpis(DashboardOverviewVO overview, ModelRuntimeVO modelRuntime) {
        Kpis kpis = new Kpis();
        kpis.setTotalTasks(safeInt(overview.todayAnalysisTaskCount()));
        kpis.setReviewRate(toDouble(overview.reviewPassRate()));
        kpis.setAvgUncertainty(toDouble(modelRuntime.highUncertaintyRate()));
        return kpis;
    }

    private List<ModelCapability> buildModelCapability(ModelRuntimeVO modelRuntime) {
        return List.of(
                new ModelCapability("Task Success", toDouble(modelRuntime.successRate()), 0.0),
                new ModelCapability("Callback Success", toDouble(modelRuntime.callbackSuccessRate()), 0.0),
                new ModelCapability("Doctor Agreement", toDouble(modelRuntime.doctorReviewAgreementRate()), 0.0));
    }

    private List<SystemEvent> buildSystemEvents(ModelRuntimeVO modelRuntime) {
        String modelVersion = firstNonBlank(modelRuntime.currentModelVersion(), "UNKNOWN");
        String message = "Runtime metrics loaded for model version " + modelVersion;
        return List.of(new SystemEvent(
                "runtime-metrics",
                LocalTime.now().format(TIME_FORMAT),
                "MODEL_RUNTIME",
                message,
                "INFO"));
    }

    private EvalSummary buildEvalSummary(ModelRuntimeVO modelRuntime) {
        EvalSummary evalSummary = new EvalSummary();
        evalSummary.setDatasetName("runtime:last-30-days");
        evalSummary.setCitationAccuracy(null);
        evalSummary.setGraphPathHitRate(null);
        evalSummary.setRefusalPrecision(null);
        evalSummary.setGroundednessRate(toDouble(modelRuntime.callbackSuccessRate()));
        evalSummary.setAvgLatencyMs(toDouble(modelRuntime.averageInferenceMillis()));
        return evalSummary;
    }

    private int safeInt(long value) {
        if (value <= 0) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private String firstNonBlank(String value, String defaultValue) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return defaultValue;
    }
}
