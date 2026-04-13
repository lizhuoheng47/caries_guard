package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.FollowupTaskSummaryVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import org.springframework.stereotype.Service;

@Service
public class DashboardFollowupStatsAppService {

    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardFollowupStatsAppService(DashboardStatsRepository dashboardStatsRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public FollowupTaskSummaryVO getFollowupTaskSummary() {
        return dashboardStatsRepository.queryFollowupTaskSummary(SecurityContextUtils.currentUser().getOrgId());
    }
}
