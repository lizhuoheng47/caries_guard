package com.cariesguard.report.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.service.ObjectStorageService;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportAttachmentModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import com.cariesguard.report.domain.repository.ReportExportLogRepository;
import com.cariesguard.report.domain.repository.ReportRecordRepository;
import com.cariesguard.report.domain.repository.ReportSourceQueryRepository;
import com.cariesguard.report.domain.service.ReportDomainService;
import com.cariesguard.report.infrastructure.service.ReportPdfService;
import com.cariesguard.report.infrastructure.service.ReportRenderService;
import com.cariesguard.report.infrastructure.service.ReportTemplateResolver;
import com.cariesguard.report.interfaces.command.ExportReportCommand;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import com.cariesguard.report.interfaces.vo.ReportExportResultVO;
import com.cariesguard.report.interfaces.vo.ReportGenerateResultVO;
import java.io.IOException;
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
class ReportAppServiceTests {

    @Mock
    private ReportSourceQueryRepository reportSourceQueryRepository;
    @Mock
    private ReportRecordRepository reportRecordRepository;
    @Mock
    private ReportExportLogRepository reportExportLogRepository;
    @Mock
    private ReportTemplateResolver reportTemplateResolver;
    @Mock
    private ReportRenderService reportRenderService;
    @Mock
    private ReportPdfService reportPdfService;
    @Mock
    private ObjectStorageService objectStorageService;
    @Mock
    private CaseCommandAppService caseCommandAppService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateReportShouldCreateArchiveAndTransitionCase() throws IOException {
        ReportAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(reportSourceQueryRepository.findCase(3001L)).thenReturn(Optional.of(
                new ReportCaseModel(3001L, "CASE202604130001", 4001L, "REVIEW_PENDING", 2001L)));
        when(reportSourceQueryRepository.findLatestSummary(3001L)).thenReturn(Optional.of(
                new ReportAnalysisSummaryModel(1L, "{\"k\":1}", "C2", new BigDecimal("0.18"), "1")));
        when(reportSourceQueryRepository.listCaseImages(3001L)).thenReturn(List.of(
                new ReportImageModel(5001L, 6001L, "PANORAMIC", "PASS", "1", "caries-image", "attachments/a.jpg", "a.jpg")));
        when(reportSourceQueryRepository.findLatestRiskAssessment(3001L)).thenReturn(Optional.of(
                new ReportRiskAssessmentModel(9001L, "HIGH", "{\"risk\":1}", 30, LocalDateTime.now())));
        when(reportSourceQueryRepository.findLatestCorrection(3001L)).thenReturn(Optional.empty());
        when(reportRecordRepository.nextVersionNo(3001L, "DOCTOR")).thenReturn(1);
        when(reportTemplateResolver.resolveContent(2001L, "DOCTOR")).thenReturn("template");
        when(reportRenderService.render(any(), any(), any())).thenReturn("rendered");
        when(reportPdfService.generatePdf("rendered")).thenReturn(new byte[] {1, 2, 3});
        when(objectStorageService.store(any(), any(), any(), any(Long.class), any())).thenReturn(
                new StoredObject("caries-image", "attachments/rpt.pdf", "rpt.pdf", "application/pdf", 3L, "md5", "MINIO"));

        ReportGenerateResultVO result = appService.generateReport(3001L, new GenerateReportCommand("DOCTOR", "Looks good", null));

        assertThat(result.reportTypeCode()).isEqualTo("DOCTOR");
        assertThat(result.versionNo()).isEqualTo(1);
        assertThat(result.reportStatusCode()).isEqualTo("FINAL");
        verify(reportRecordRepository).create(any());
        verify(reportRecordRepository).createAttachment(any());
        verify(reportRecordRepository).updateArchiveInfo(any(), any(), eq("FINAL"), any(), eq(1001L));
        verify(caseCommandAppService).transitionStatus(eq(3001L), any());
    }

    @Test
    void exportReportShouldWriteAuditLog() {
        ReportAppService appService = createService();
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        when(reportRecordRepository.findById(8001L)).thenReturn(Optional.of(
                new ReportRecordModel(8001L, "RPT202604130001", 3001L, 4001L, 7001L, "DOCTOR", "FINAL", 1,
                        "summary", LocalDateTime.now(), null, 2001L, LocalDateTime.now())));
        when(reportRecordRepository.findAttachment(7001L)).thenReturn(Optional.of(
                new ReportAttachmentModel(7001L, "caries-image", "attachments/rpt.pdf", "rpt.pdf", "application/pdf", 2001L, "ACTIVE")));

        ReportExportResultVO result = appService.exportReport(8001L, new ExportReportCommand(null, null));

        assertThat(result.reportId()).isEqualTo(8001L);
        assertThat(result.exported()).isTrue();
        verify(reportExportLogRepository).create(any());
    }

    private ReportAppService createService() {
        return new ReportAppService(
                reportSourceQueryRepository,
                reportRecordRepository,
                reportExportLogRepository,
                new ReportDomainService(),
                reportTemplateResolver,
                reportRenderService,
                reportPdfService,
                objectStorageService,
                caseCommandAppService);
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}

