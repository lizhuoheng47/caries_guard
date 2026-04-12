package com.cariesguard.analysis.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisPatientModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.repository.AnalysisQueryRepository;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AnalysisTaskAppServiceTests {

    @Mock
    private AnalysisCommandRepository analysisCommandRepository;

    @Mock
    private AnalysisQueryRepository analysisQueryRepository;

    @Mock
    private AnalysisTaskEventPublisher analysisTaskEventPublisher;

    @Mock
    private CaseCommandAppService caseCommandAppService;

    private AnalysisProperties analysisProperties;

    @BeforeEach
    void setUp() {
        analysisProperties = new AnalysisProperties();
        analysisProperties.setDefaultModelVersion("caries-v1");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTaskShouldPersistAndTransitionCase() {
        AnalysisTaskAppService appService = new AnalysisTaskAppService(
                analysisCommandRepository,
                analysisQueryRepository,
                analysisTaskEventPublisher,
                caseCommandAppService,
                analysisProperties,
                new ObjectMapper());
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "QC_PENDING")));
        when(analysisCommandRepository.findPatient(2001L)).thenReturn(Optional.of(
                new AnalysisPatientModel(2001L, 12, "MALE")));
        when(analysisCommandRepository.listImages(3001L, List.of(5001L))).thenReturn(List.of(
                new AnalysisImageModel(5001L, 4001L, "PANORAMIC", "PASS", "caries-image", "attachments/x.jpg")));

        AnalysisTaskVO result = appService.createTask(3001L, new CreateAnalysisTaskCommand(List.of(5001L), "INFERENCE"));

        assertThat(result.taskStatusCode()).isEqualTo("QUEUEING");
        verify(analysisCommandRepository).createTask(any());
        verify(analysisTaskEventPublisher).publishRequested(any());
        verify(caseCommandAppService).transitionStatus(eq(3001L), any());
    }

    @Test
    void createTaskShouldRejectCaseNotReady() {
        AnalysisTaskAppService appService = new AnalysisTaskAppService(
                analysisCommandRepository,
                analysisQueryRepository,
                analysisTaskEventPublisher,
                caseCommandAppService,
                analysisProperties,
                new ObjectMapper());
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "CREATED")));

        assertThatThrownBy(() -> appService.createTask(3001L, new CreateAnalysisTaskCommand(List.of(5001L), "INFERENCE")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getTaskShouldReturnView() {
        AnalysisTaskAppService appService = new AnalysisTaskAppService(
                analysisCommandRepository,
                analysisQueryRepository,
                analysisTaskEventPublisher,
                caseCommandAppService,
                analysisProperties,
                new ObjectMapper());
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(analysisQueryRepository.findTask(6001L)).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "QUEUEING", null,
                        LocalDateTime.now(), null, null, 100001L)));

        AnalysisTaskVO result = appService.getTask(6001L);

        assertThat(result.taskNo()).isEqualTo("TASK1");
        assertThat(result.taskStatusCode()).isEqualTo("QUEUEING");
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
