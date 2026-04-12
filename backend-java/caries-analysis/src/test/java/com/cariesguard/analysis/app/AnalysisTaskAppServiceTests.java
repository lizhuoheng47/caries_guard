package com.cariesguard.analysis.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.analysis.domain.model.AnalysisCaseModel;
import com.cariesguard.analysis.domain.model.AnalysisImageModel;
import com.cariesguard.analysis.domain.model.AnalysisPatientModel;
import com.cariesguard.analysis.domain.model.AnalysisTaskViewModel;
import com.cariesguard.analysis.domain.repository.AnaTaskRecordRepository;
import com.cariesguard.analysis.domain.repository.AnalysisCommandRepository;
import com.cariesguard.analysis.domain.service.AnalysisIdempotencyDomainService;
import com.cariesguard.analysis.domain.service.AnalysisTaskDomainService;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.command.RetryAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AnalysisTaskAppServiceTests {

    @Mock
    private AnalysisCommandRepository analysisCommandRepository;

    @Mock
    private AnaTaskRecordRepository anaTaskRecordRepository;

    @Mock
    private AnalysisTaskEventPublisher analysisTaskEventPublisher;

    @Mock
    private CaseCommandAppService caseCommandAppService;

    @Mock
    private AnalysisIdempotencyDomainService analysisIdempotencyDomainService;

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

    private AnalysisTaskAppService createService() {
        return new AnalysisTaskAppService(
                analysisCommandRepository,
                anaTaskRecordRepository,
                analysisTaskEventPublisher,
                new AnalysisTaskDomainService(),
                analysisIdempotencyDomainService,
                caseCommandAppService,
                analysisProperties,
                new ObjectMapper());
    }

    @Test
    void createTaskShouldPersistAndTransitionCase() {
        AnalysisTaskAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "QC_PENDING")));
        when(analysisCommandRepository.findPatient(2001L)).thenReturn(Optional.of(
                new AnalysisPatientModel(2001L, 12, "MALE")));
        when(analysisCommandRepository.listCaseImages(3001L)).thenReturn(List.of(
                new AnalysisImageModel(5001L, 3001L, 4001L, "PANORAMIC", "PASS", "caries-image", "attachments/x.jpg")));
        when(anaTaskRecordRepository.existsRunningTaskByCaseId(3001L)).thenReturn(false);

        AnalysisTaskVO result = appService.createTask(new CreateAnalysisTaskCommand(3001L, 2001L, false, "INFERENCE", null));

        assertThat(result.taskStatusCode()).isEqualTo("QUEUEING");
        verify(anaTaskRecordRepository).save(any());
        verify(analysisTaskEventPublisher).publishRequested(any());
        verify(caseCommandAppService).transitionStatus(eq(3001L), any());
    }

    @Test
    void createTaskShouldRejectCaseNotReady() {
        AnalysisTaskAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "CREATED")));

        assertThatThrownBy(() -> appService.createTask(new CreateAnalysisTaskCommand(3001L, 2001L, false, "INFERENCE", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createTaskShouldRejectRunningTask() {
        AnalysisTaskAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "QC_PENDING")));
        when(anaTaskRecordRepository.existsRunningTaskByCaseId(3001L)).thenReturn(true);

        assertThatThrownBy(() -> appService.createTask(new CreateAnalysisTaskCommand(3001L, 2001L, false, "INFERENCE", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createTaskShouldRejectNoApprovedImage() {
        AnalysisTaskAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "QC_PENDING")));
        when(anaTaskRecordRepository.existsRunningTaskByCaseId(3001L)).thenReturn(false);
        when(analysisCommandRepository.listCaseImages(3001L)).thenReturn(List.of(
                new AnalysisImageModel(5001L, 3001L, 4001L, "PANORAMIC", "REJECT", "caries-image", "attachments/x.jpg")));

        assertThatThrownBy(() -> appService.createTask(new CreateAnalysisTaskCommand(3001L, 2001L, false, "INFERENCE", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createTaskShouldRejectCrossOrgAccess() {
        AnalysisTaskAppService appService = createService();
        // User belongs to org 200001, case belongs to org 100001
        setCurrentUser(new AuthenticatedUser(1001L, 200001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "QC_PENDING")));

        assertThatThrownBy(() -> appService.createTask(new CreateAnalysisTaskCommand(3001L, 2001L, false, "INFERENCE", null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void retryTaskShouldCreateNewTaskFromFailedOne() {
        AnalysisTaskAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(anaTaskRecordRepository.findById(6001L)).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "FAILED", "timeout",
                        LocalDateTime.now(), null, null, 100001L, null)));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "QC_PENDING")));
        when(analysisCommandRepository.findPatient(2001L)).thenReturn(Optional.of(
                new AnalysisPatientModel(2001L, 12, "MALE")));
        when(analysisCommandRepository.listCaseImages(3001L)).thenReturn(List.of(
                new AnalysisImageModel(5001L, 3001L, 4001L, "PANORAMIC", "PASS", "caries-image", "attachments/x.jpg")));

        AnalysisTaskVO result = appService.retryTask(new RetryAnalysisTaskCommand(6001L, "RETRY", "timeout recovery"));

        assertThat(result.taskStatusCode()).isEqualTo("QUEUEING");
        // Verify new task saved with retryFromTaskId
        ArgumentCaptor<com.cariesguard.analysis.domain.model.AnalysisTaskCreateModel> captor =
                ArgumentCaptor.forClass(com.cariesguard.analysis.domain.model.AnalysisTaskCreateModel.class);
        verify(anaTaskRecordRepository).save(captor.capture());
        assertThat(captor.getValue().retryFromTaskId()).isEqualTo(6001L);
        verify(analysisTaskEventPublisher).publishRequested(any());
        ArgumentCaptor<CaseStatusTransitionCommand> transitionCaptor = ArgumentCaptor.forClass(CaseStatusTransitionCommand.class);
        verify(caseCommandAppService).transitionStatus(eq(3001L), transitionCaptor.capture());
        assertThat(transitionCaptor.getValue().reasonCode()).isEqualTo("RETRY");
    }

    @Test
    void retryTaskShouldRejectNonFailedTask() {
        AnalysisTaskAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(anaTaskRecordRepository.findById(6001L)).thenReturn(Optional.of(
                new AnalysisTaskViewModel(6001L, "TASK1", 3001L, 2001L, "caries-v1", "INFERENCE", "SUCCESS", null,
                        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), 100001L, null)));
        org.mockito.Mockito.doThrow(new BusinessException("B0001", "Only FAILED tasks may be retried"))
                .when(analysisIdempotencyDomainService).ensureRetryAllowed(any());

        assertThatThrownBy(() -> appService.retryTask(new RetryAnalysisTaskCommand(6001L, "RETRY", "test")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createTaskShouldNotPublishEventWhenCaseTransitionFails() {
        AnalysisTaskAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(100001L, 100001L, "admin", "hash", "Admin", true, List.of("SYS_ADMIN")));
        when(analysisCommandRepository.findCase(3001L)).thenReturn(Optional.of(
                new AnalysisCaseModel(3001L, "CASE1", 2001L, 100001L, "QC_PENDING")));
        when(analysisCommandRepository.findPatient(2001L)).thenReturn(Optional.of(
                new AnalysisPatientModel(2001L, 12, "MALE")));
        when(analysisCommandRepository.listCaseImages(3001L)).thenReturn(List.of(
                new AnalysisImageModel(5001L, 3001L, 4001L, "PANORAMIC", "PASS", "caries-image", "attachments/x.jpg")));
        when(anaTaskRecordRepository.existsRunningTaskByCaseId(3001L)).thenReturn(false);
        org.mockito.Mockito.doThrow(new BusinessException("B0001", "transition failed"))
                .when(caseCommandAppService).transitionStatus(eq(3001L), any(CaseStatusTransitionCommand.class));

        assertThatThrownBy(() -> appService.createTask(new CreateAnalysisTaskCommand(3001L, 2001L, false, "INFERENCE", null)))
                .isInstanceOf(BusinessException.class);
        verify(analysisTaskEventPublisher, never()).publishRequested(any());
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
