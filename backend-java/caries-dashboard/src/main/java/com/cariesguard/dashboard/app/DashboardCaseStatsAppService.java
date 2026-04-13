package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.CaseStatusDistributionVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import org.springframework.stereotype.Service;

@Service
public class DashboardCaseStatsAppService {

    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardCaseStatsAppService(DashboardStatsRepository dashboardStatsRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public CaseStatusDistributionVO getCaseStatusDistribution() {
        return dashboardStatsRepository.queryCaseStatusDistribution(SecurityContextUtils.currentUser().getOrgId());
    }
}
