package com.cariesguard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.integration.support.AnalysisReportE2EFixture;
import com.cariesguard.report.domain.model.ReportExportLogModel;
import com.cariesguard.report.domain.model.ReportRecordModel;
import com.cariesguard.report.interfaces.command.ExportReportCommand;
import com.cariesguard.report.interfaces.command.GenerateReportCommand;
import com.cariesguard.report.interfaces.vo.ReportExportResultVO;
import com.cariesguard.report.interfaces.vo.ReportGenerateResultVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReportExportAuditIntegrationTest {

    private final AnalysisReportE2EFixture fixture = AnalysisReportE2EFixture.createDefault();

    @AfterEach
    void tearDown() {
        fixture.clearSecurityContext();
    }

    @Test
    void shouldPersistExportAuditAfterReportExport() {
        fixture.setCurrentUser(new AuthenticatedUser(
                100001L, fixture.state.orgId, "doctor", "hash", "Doctor", true, List.of("DOCTOR")));

        AnalysisTaskVO task = fixture.analysisTaskAppService.createTask(new CreateAnalysisTaskCommand(
                fixture.state.caseId, fixture.state.patientId, false, "INFERENCE", "export-audit"));
        String callbackBody = fixture.buildSuccessCallbackBody(task.taskNo());
        String timestamp = fixture.currentEpochSecond();
        fixture.analysisCallbackAppService.handleResultCallback(callbackBody, timestamp, fixture.signCallback(callbackBody, timestamp));

        ReportGenerateResultVO report = fixture.reportAppService.generateReport(
                fixture.state.caseId,
                new GenerateReportCommand("DOCTOR", "Ready for export", null));
        ReportRecordModel reportRecord = fixture.reportRecordRepository.findById(report.reportId()).orElseThrow();
        assertThat(reportRecord.attachmentId()).isNotNull();

        ReportExportResultVO exportResult = fixture.reportAppService.exportReport(report.reportId(), new ExportReportCommand("PDF", "DOWNLOAD"));

        assertThat(exportResult.exported()).isTrue();
        assertThat(exportResult.reportId()).isEqualTo(report.reportId());
        assertThat(fixture.reportExportLogRepository.logs()).hasSize(1);
        ReportExportLogModel exportLog = fixture.reportExportLogRepository.logs().get(0);
        assertThat(exportLog.reportId()).isEqualTo(report.reportId());
        assertThat(exportLog.attachmentId()).isEqualTo(exportResult.attachmentId());
        assertThat(exportLog.attachmentId()).isNotEqualTo(reportRecord.attachmentId());
        assertThat(exportLog.exportTypeCode()).isEqualTo("PDF");
        assertThat(exportLog.exportChannelCode()).isEqualTo("DOWNLOAD");
        assertThat(exportLog.exportedBy()).isNotNull();
        assertThat(exportLog.exportedAt()).isNotNull();
        assertThat(exportLog.orgId()).isEqualTo(fixture.state.orgId);
    }
}

