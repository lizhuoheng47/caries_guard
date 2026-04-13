package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.RiskLevelDistributionVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import org.springframework.stereotype.Service;

@Service
public class DashboardRiskStatsAppService {

    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardRiskStatsAppService(DashboardStatsRepository dashboardStatsRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public RiskLevelDistributionVO getRiskLevelDistribution() {
        return dashboardStatsRepository.queryRiskLevelDistribution(SecurityContextUtils.currentUser().getOrgId());
    }
}
