package com.cariesguard.dashboard.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.dashboard.app.DashboardBacklogAppService;
import com.cariesguard.dashboard.app.DashboardCaseStatsAppService;
import com.cariesguard.dashboard.app.DashboardFollowupStatsAppService;
import com.cariesguard.dashboard.app.DashboardOverviewAppService;
import com.cariesguard.dashboard.app.DashboardRiskStatsAppService;
import com.cariesguard.dashboard.app.DashboardTrendAppService;
import com.cariesguard.dashboard.interfaces.vo.BacklogSummaryVO;
import com.cariesguard.dashboard.interfaces.vo.CaseStatusDistributionVO;
import com.cariesguard.dashboard.interfaces.vo.DashboardOverviewVO;
import com.cariesguard.dashboard.interfaces.vo.FollowupTaskSummaryVO;
import com.cariesguard.dashboard.interfaces.vo.RiskLevelDistributionVO;
import com.cariesguard.dashboard.interfaces.vo.DashboardTrendPointVO;
import java.time.LocalDate;
import java.util.List;
import com.cariesguard.framework.security.authorization.RequirePermission;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardOverviewAppService dashboardOverviewAppService;
    private final DashboardCaseStatsAppService dashboardCaseStatsAppService;
    private final DashboardRiskStatsAppService dashboardRiskStatsAppService;
    private final DashboardFollowupStatsAppService dashboardFollowupStatsAppService;
    private final DashboardBacklogAppService dashboardBacklogAppService;
    private final DashboardTrendAppService dashboardTrendAppService;

    public DashboardController(DashboardOverviewAppService dashboardOverviewAppService,
                               DashboardCaseStatsAppService dashboardCaseStatsAppService,
                               DashboardRiskStatsAppService dashboardRiskStatsAppService,
                               DashboardFollowupStatsAppService dashboardFollowupStatsAppService,
                               DashboardBacklogAppService dashboardBacklogAppService,
                               DashboardTrendAppService dashboardTrendAppService) {
        this.dashboardOverviewAppService = dashboardOverviewAppService;
        this.dashboardCaseStatsAppService = dashboardCaseStatsAppService;
        this.dashboardRiskStatsAppService = dashboardRiskStatsAppService;
        this.dashboardFollowupStatsAppService = dashboardFollowupStatsAppService;
        this.dashboardBacklogAppService = dashboardBacklogAppService;
        this.dashboardTrendAppService = dashboardTrendAppService;
    }

    @GetMapping("/api/v1/dashboard/overview")
    @RequirePermission("dashboard:view")
    public ApiResponse<DashboardOverviewVO> getOverview() {
        return ApiResponse.success(dashboardOverviewAppService.getOverview(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/dashboard/case-status-distribution")
    @RequirePermission("dashboard:view")
    public ApiResponse<CaseStatusDistributionVO> getCaseStatusDistribution() {
        return ApiResponse.success(dashboardCaseStatsAppService.getCaseStatusDistribution(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/dashboard/risk-level-distribution")
    @RequirePermission("dashboard:view")
    public ApiResponse<RiskLevelDistributionVO> getRiskLevelDistribution() {
        return ApiResponse.success(dashboardRiskStatsAppService.getRiskLevelDistribution(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/dashboard/followup-task-summary")
    @RequirePermission("dashboard:view")
    public ApiResponse<FollowupTaskSummaryVO> getFollowupTaskSummary() {
        return ApiResponse.success(dashboardFollowupStatsAppService.getFollowupTaskSummary(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/dashboard/backlog-summary")
    @RequirePermission("dashboard:view")
    public ApiResponse<BacklogSummaryVO> getBacklogSummary() {
        return ApiResponse.success(dashboardBacklogAppService.getBacklogSummary(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/dashboard/trend")
    @RequirePermission("dashboard:view")
    public ApiResponse<List<DashboardTrendPointVO>> getTrend(
            @RequestParam(defaultValue = "LAST_7_DAYS") String rangeType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(
                dashboardTrendAppService.getTrend(rangeType, startDate, endDate),
                TraceIdUtils.currentTraceId());
    }
}
