package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.CorrectionFeedbackStatsVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import org.springframework.stereotype.Service;

@Service
public class DashboardCorrectionFeedbackAppService {

    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardCorrectionFeedbackAppService(DashboardStatsRepository dashboardStatsRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public CorrectionFeedbackStatsVO getCorrectionFeedbackStats() {
        return dashboardStatsRepository.queryCorrectionFeedbackStats(SecurityContextUtils.currentUser().getOrgId());
    }
}
