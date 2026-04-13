package com.cariesguard.dashboard.app;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.query.DashboardRangeQuery;
import com.cariesguard.dashboard.interfaces.vo.DashboardTrendPointVO;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardTrendAppService {

    private final DashboardStatsRepository dashboardStatsRepository;

    public DashboardTrendAppService(DashboardStatsRepository dashboardStatsRepository) {
        this.dashboardStatsRepository = dashboardStatsRepository;
    }

    public List<DashboardTrendPointVO> getTrend(String rangeType, LocalDate startDate, LocalDate endDate) {
        DashboardRangeQuery rangeQuery = DashboardRangeQuery.of(rangeType, startDate, endDate);
        return dashboardStatsRepository.queryTrend(SecurityContextUtils.currentUser().getOrgId(), rangeQuery);
    }
}
