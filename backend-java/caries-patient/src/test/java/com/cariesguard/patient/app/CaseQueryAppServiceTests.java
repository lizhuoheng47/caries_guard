package com.cariesguard.patient.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.domain.model.CaseDetailModel;
import com.cariesguard.patient.domain.model.CaseDiagnosisModel;
import com.cariesguard.patient.domain.model.CaseImageModel;
import com.cariesguard.patient.domain.model.CaseSummaryModel;
import com.cariesguard.patient.domain.model.PageQueryResult;
import com.cariesguard.patient.domain.repository.CaseQueryRepository;
import com.cariesguard.patient.interfaces.vo.CaseDetailVO;
import com.cariesguard.patient.interfaces.vo.CaseListItemVO;
import com.cariesguard.patient.interfaces.vo.PageResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class CaseQueryAppServiceTests {

    @Mock
    private CaseQueryRepository caseQueryRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCaseShouldReturnAggregatedPayload() {
        CaseQueryAppService appService = new CaseQueryAppService(caseQueryRepository, new ObjectMapper());
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(caseQueryRepository.findCaseDetail(6001L)).thenReturn(Optional.of(new CaseDetailModel(
                6001L,
                "CASE202604120001",
                3001L,
                4001L,
                "REVIEW_PENDING",
                "0",
                "0",
                2001L,
                List.of(new CaseImageModel(7001L, "PANORAMIC", "PENDING", "1")),
                List.of(new CaseDiagnosisModel("caries", "MEDIUM", "1")),
                "{\"score\":80}")));

        CaseDetailVO result = appService.getCase(6001L);

        assertThat(result.images()).hasSize(1);
        assertThat(result.diagnoses()).hasSize(1);
        assertThat(result.latestAiSummary().get("score").asInt()).isEqualTo(80);
    }

    @Test
    void getCaseShouldRejectCrossOrgAccess() {
        CaseQueryAppService appService = new CaseQueryAppService(caseQueryRepository, new ObjectMapper());
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(caseQueryRepository.findCaseDetail(6001L)).thenReturn(Optional.of(new CaseDetailModel(
                6001L,
                "CASE202604120001",
                3001L,
                4001L,
                "REVIEW_PENDING",
                "0",
                "0",
                9999L,
                List.of(),
                List.of(),
                null)));

        assertThatThrownBy(() -> appService.getCase(6001L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void pageCasesShouldReturnScopedSummaries() {
        CaseQueryAppService appService = new CaseQueryAppService(caseQueryRepository, new ObjectMapper());
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(caseQueryRepository.pageCases(2001L, 1, 10, 3001L, "CREATED", 21L))
                .thenReturn(new PageQueryResult<>(
                        List.of(new CaseSummaryModel(
                                6001L,
                                "CASE202604120001",
                                3001L,
                                4001L,
                                "screening case",
                                "CREATED",
                                "NORMAL",
                                21L,
                                "0",
                                "0",
                                java.time.LocalDateTime.of(2026, 4, 12, 10, 0))),
                        1L,
                        1,
                        10));

        PageResultVO<CaseListItemVO> result = appService.pageCases(1, 10, 3001L, "CREATED", 21L);

        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).caseNo()).isEqualTo("CASE202604120001");
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
