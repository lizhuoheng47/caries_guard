package com.cariesguard.patient.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.PageQueryResult;
import com.cariesguard.patient.domain.model.VisitDetailModel;
import com.cariesguard.patient.domain.model.VisitSummaryModel;
import com.cariesguard.patient.domain.repository.VisitQueryRepository;
import com.cariesguard.patient.interfaces.vo.PageResultVO;
import com.cariesguard.patient.interfaces.vo.VisitDetailVO;
import com.cariesguard.patient.interfaces.vo.VisitListItemVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class VisitQueryAppServiceTests {

    @Mock
    private VisitQueryRepository visitQueryRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getVisitShouldReturnOwnedRecord() {
        VisitQueryAppService appService = new VisitQueryAppService(visitQueryRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitQueryRepository.findVisitDetail(4001L)).thenReturn(Optional.of(new VisitDetailModel(
                4001L,
                "VIS202604120001",
                3001L,
                10L,
                21L,
                "SCREENING",
                LocalDateTime.of(2026, 4, 12, 9, 0),
                "screening",
                "NORMAL",
                "MANUAL",
                2001L,
                "ACTIVE",
                null)));

        VisitDetailVO result = appService.getVisit(4001L);

        assertThat(result.visitNo()).isEqualTo("VIS202604120001");
        assertThat(result.patientId()).isEqualTo(3001L);
    }

    @Test
    void getVisitShouldRejectCrossOrgAccess() {
        VisitQueryAppService appService = new VisitQueryAppService(visitQueryRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitQueryRepository.findVisitDetail(4001L)).thenReturn(Optional.of(new VisitDetailModel(
                4001L,
                "VIS202604120001",
                3001L,
                10L,
                21L,
                "SCREENING",
                LocalDateTime.of(2026, 4, 12, 9, 0),
                "screening",
                "NORMAL",
                "MANUAL",
                9999L,
                "ACTIVE",
                null)));

        assertThatThrownBy(() -> appService.getVisit(4001L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void pageVisitsShouldReturnScopedResult() {
        VisitQueryAppService appService = new VisitQueryAppService(visitQueryRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(visitQueryRepository.pageVisits(2001L, 1, 10, 3001L, 21L, "SCREENING"))
                .thenReturn(new PageQueryResult<>(
                        List.of(new VisitSummaryModel(
                                4001L,
                                "VIS202604120001",
                                3001L,
                                21L,
                                "SCREENING",
                                LocalDateTime.of(2026, 4, 12, 9, 0),
                                "NORMAL",
                                "ACTIVE")),
                        1L,
                        1,
                        10));

        PageResultVO<VisitListItemVO> result = appService.pageVisits(1, 10, 3001L, 21L, "SCREENING");

        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).visitNo()).isEqualTo("VIS202604120001");
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
