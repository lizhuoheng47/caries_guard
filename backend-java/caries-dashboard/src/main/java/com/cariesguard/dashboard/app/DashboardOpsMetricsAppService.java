package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.ModelRuntimeVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import org.springframework.stereotype.Service;

@Service
public class DashboardOpsMetricsAppService {

    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardOpsMetricsAppService(DashboardStatsRepository dashboardStatsRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public ModelRuntimeVO getModelRuntime() {
        return dashboardStatsRepository.queryModelRuntime(SecurityContextUtils.currentUser().getOrgId());
    }
}
