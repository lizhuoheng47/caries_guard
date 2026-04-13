package com.cariesguard.dashboard.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.dashboard.app.DashboardOpsMetricsAppService;
import com.cariesguard.dashboard.interfaces.vo.ModelRuntimeVO;
import com.cariesguard.framework.security.authorization.RequirePermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardOpsController {

    private final DashboardOpsMetricsAppService dashboardOpsMetricsAppService;

    public DashboardOpsController(DashboardOpsMetricsAppService dashboardOpsMetricsAppService) {
        this.dashboardOpsMetricsAppService = dashboardOpsMetricsAppService;
    }

    @GetMapping("/api/v1/dashboard/model-runtime")
    @RequirePermission("dashboard:ops:view")
    public ApiResponse<ModelRuntimeVO> getModelRuntime() {
        return ApiResponse.success(dashboardOpsMetricsAppService.getModelRuntime(), TraceIdUtils.currentTraceId());
    }
}
