package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.BacklogSummaryVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import org.springframework.stereotype.Service;

@Service
public class DashboardBacklogAppService {

    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardBacklogAppService(DashboardStatsRepository dashboardStatsRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public BacklogSummaryVO getBacklogSummary() {
        return dashboardStatsRepository.queryBacklogSummary(SecurityContextUtils.currentUser().getOrgId());
    }
}
