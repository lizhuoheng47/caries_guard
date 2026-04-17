package com.cariesguard.report.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.domain.model.ReportAnalysisSummaryModel;
import com.cariesguard.report.domain.model.ReportCaseModel;
import com.cariesguard.report.domain.model.ReportCorrectionModel;
import com.cariesguard.report.domain.model.ReportImageModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.domain.model.ReportRiskAssessmentModel;
import com.cariesguard.report.domain.model.ReportToothRecordModel;
import com.cariesguard.report.domain.model.ReportVisualAssetModel;
import com.cariesguard.report.domain.repository.ReportRecordRepository;
import com.cariesguard.report.domain.repository.ReportSourceQueryRepository;
import com.cariesguard.report.interfaces.vo.ReportDetailVO;
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
class ReportQueryAppServiceTests {

    @Mock
    private ReportRecordRepository reportRecordRepository;
    @Mock
    private ReportSourceQueryRepository reportSourceQueryRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getReportShouldReturnPreviewSnapshotFromFrozenSources() {
        ReportQueryAppService appService = new ReportQueryAppService(reportRecordRepository, reportSourceQueryRepository);
        setCurrentUser(new AuthenticatedUser(1001L, 2001L, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));
        LocalDateTime now = LocalDateTime.now();
        when(reportRecordRepository.findById(8001L)).thenReturn(Optional.of(
                new ReportRecordModel(8001L, "RPT202604170001", 3001L, 4001L, 7001L,
                        9101L, 9201L, 9301L, "DOCTOR", "FINAL", 2,
                        "summary", now, null, 2001L, now)));
        when(reportSourceQueryRepository.findCase(3001L)).thenReturn(Optional.of(
                new ReportCaseModel(3001L, "CASE202604170001", 4001L, "REPORT_READY", 2001L)));
        when(reportSourceQueryRepository.findSummaryById(9101L)).thenReturn(Optional.of(
                new ReportAnalysisSummaryModel(9101L, 6001L, "{\"visualAssets\":[]}", "C2", new BigDecimal("0.22"), "1", 3, 2)));
        when(reportSourceQueryRepository.findRiskAssessmentById(9201L)).thenReturn(Optional.of(
                new ReportRiskAssessmentModel(9201L, "HIGH", "{\"risk\":1}", 30, now)));
        when(reportSourceQueryRepository.listCaseImages(3001L)).thenReturn(List.of(
                new ReportImageModel(5001L, 6001L, "PANORAMIC", "PASS", "1", "caries-image", "image/a.jpg", "a.jpg")));
        when(reportSourceQueryRepository.listToothRecords(3001L)).thenReturn(List.of(
                new ReportToothRecordModel(5101L, 5001L, "16", "OCCLUSAL", "CARIES", "C2", "suspected lesion", "review", 0)));
        when(reportSourceQueryRepository.listVisualAssetsByTaskId(6001L)).thenReturn(List.of(
                new ReportVisualAssetModel(5201L, 6001L, "OVERLAY", 6201L, 5001L, 6001L, "16", 0,
                        "caries-visual", "visual/overlay.png", "image/png", "overlay.png")));
        when(reportSourceQueryRepository.listCorrections(3001L)).thenReturn(List.of(
                new ReportCorrectionModel(9301L, "RE_GRADE", "{\"tooth\":\"16\"}", now)));

        ReportDetailVO detail = appService.getReport(8001L);

        assertThat(detail.reportNo()).isEqualTo("RPT202604170001");
        assertThat(detail.caseNo()).isEqualTo("CASE202604170001");
        assertThat(detail.sourceSummaryId()).isEqualTo(9101L);
        assertThat(detail.analysisSummary().taskId()).isEqualTo(6001L);
        assertThat(detail.riskAssessment().overallRiskLevelCode()).isEqualTo("HIGH");
        assertThat(detail.images()).hasSize(1);
        assertThat(detail.toothRecords()).hasSize(1);
        assertThat(detail.visualAssets()).hasSize(1);
        assertThat(detail.corrections()).hasSize(1);
    }

    private void setCurrentUser(AuthenticatedUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()));
    }
}
