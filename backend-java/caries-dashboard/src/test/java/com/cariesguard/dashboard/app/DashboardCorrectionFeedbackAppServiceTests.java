package com.cariesguard.dashboard.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cariesguard.dashboard.infrastructure.repository.DashboardStatsRepository;
import com.cariesguard.dashboard.interfaces.vo.CorrectionFeedbackStatsVO;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DashboardCorrectionFeedbackAppServiceTests {

    @Mock
    private DashboardStatsRepository dashboardStatsRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCorrectionFeedbackStatsShouldUseCurrentUserOrg() {
        AuthenticatedUser user = new AuthenticatedUser(
                1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
        CorrectionFeedbackStatsVO stats = new CorrectionFeedbackStatsVO(
                3L,
                2L,
                1L,
                1L,
                1L,
                1L,
                1L,
                2L,
                new BigDecimal("0.6667"));
        when(dashboardStatsRepository.queryCorrectionFeedbackStats(2001L)).thenReturn(stats);

        DashboardCorrectionFeedbackAppService appService =
                new DashboardCorrectionFeedbackAppService(dashboardStatsRepository);

        CorrectionFeedbackStatsVO result = appService.getCorrectionFeedbackStats();

        assertThat(result).isEqualTo(stats);
    }
}
