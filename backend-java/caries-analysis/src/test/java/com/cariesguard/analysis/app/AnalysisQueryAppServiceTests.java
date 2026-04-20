package com.cariesguard.analysis.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.cariesguard.analysis.domain.model.AnalysisResultSummaryModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaResultSummaryRepository;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnaVisualAssetRepository;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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
class AnalysisQueryAppServiceTests {

    @Mock
    private AnaTaskRecordRepository anaTaskRecordRepository;

    @Mock
    private AnaResultSummaryRepository anaResultSummaryRepository;

    @Mock
    private AnaVisualAssetRepository anaVisualAssetRepository;

    private AnalysisQueryAppService createService() {
        return new AnalysisQueryAppService(
                anaTaskRecordRepository,
                anaResultSummaryRepository,
                anaVisualAssetRepository,
                new ObjectMapper(),
                null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTaskDetailShouldUseAggregateColumnsWhenRawPayloadIsInvalid() {
        AnalysisQueryAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(anaTaskRecordRepository.findById(6001L)).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "SUCCESS", null,
                        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 2001L, null)));
        when(anaResultSummaryRepository.findByTaskId(6001L)).thenReturn(Optional.of(
                new AnalysisResultSummaryModel(
                        7001L, 6001L, 3001L, "{invalid-json",
                        "C2", new BigDecimal("0.1800"), "1", 2001L, 1001L)));
        when(anaVisualAssetRepository.listByTaskId(6001L)).thenReturn(List.of());

        AnalysisTaskDetailVO detail = appService.getTaskDetail(6001L);

        assertThat(detail.summary()).isNotNull();
        assertThat(detail.summary().overallHighestSeverity()).isEqualTo("C2");
        assertThat(detail.summary().uncertaintyScore()).isEqualTo(0.18d);
        assertThat(detail.summary().reviewSuggestedFlag()).isEqualTo("1");
    }

    @Test
    void getTaskDetailShouldRejectInvalidRawPayloadWithoutAggregates() {
        AnalysisQueryAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(anaTaskRecordRepository.findById(6001L)).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "SUCCESS", null,
                        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 2001L, null)));
        when(anaResultSummaryRepository.findByTaskId(6001L)).thenReturn(Optional.of(
                new AnalysisResultSummaryModel(
                        7001L, 6001L, 3001L, "{invalid-json",
                        null, null, null, 2001L, 1001L)));

        assertThatThrownBy(() -> appService.getTaskDetail(6001L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getTaskDetailShouldExposeGradingAndUncertaintyFieldsFromRawResultJson() {
        AnalysisQueryAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(anaTaskRecordRepository.findById(6001L)).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "SUCCESS", null,
                        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 2001L, null)));
        when(anaResultSummaryRepository.findByTaskId(6001L)).thenReturn(Optional.of(
                new AnalysisResultSummaryModel(
                        7001L,
                        6001L,
                        3001L,
                        """
                                {
                                  "gradingLabel":"C3",
                                  "confidenceScore":0.61,
                                  "uncertaintyScore":0.72,
                                  "needsReview":true,
                                  "reviewSuggestedFlag":"1",
                                  "lesionResults":[{"toothCode":"16","severityCode":"C3"}]
                                }
                                """,
                        "C3",
                        new BigDecimal("0.7200"),
                        "1",
                        2001L,
                        1001L)));
        when(anaVisualAssetRepository.listByTaskId(6001L)).thenReturn(List.of());

        AnalysisTaskDetailVO detail = appService.getTaskDetail(6001L);

        assertThat(detail.summary()).isNotNull();
        assertThat(detail.summary().gradingLabel()).isEqualTo("C3");
        assertThat(detail.summary().confidenceScore()).isEqualTo(0.61d);
        assertThat(detail.summary().uncertaintyScore()).isEqualTo(0.72d);
        assertThat(detail.summary().needsReview()).isTrue();
        assertThat(detail.summary().reviewSuggestedFlag()).isEqualTo("1");
        assertThat(detail.summary().rawResultJson()).isNotNull();
        assertThat(detail.summary().rawResultJson().path("gradingLabel").asText()).isEqualTo("C3");
        assertThat(detail.summary().rawResultJson().path("needsReview").asBoolean()).isTrue();
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
