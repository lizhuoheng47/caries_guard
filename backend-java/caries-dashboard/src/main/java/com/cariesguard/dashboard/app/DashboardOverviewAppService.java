package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.DashboardOverviewVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import org.springframework.stereotype.Service;

@Service
public class DashboardOverviewAppService {

    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardOverviewAppService(DashboardStatsRepository dashboardStatsRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public DashboardOverviewVO getOverview() {
        return dashboardStatsRepository.queryOverview(SecurityContextUtils.currentUser().getOrgId());
    }
}
