package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class AnalysisToReportE2ETest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldRunGoldenPathFromAnalysisToReportAndExportAudit() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");
        AnalysisTaskRef task = createAnalysisTask(fixture, "e2e golden path");

        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", fixture.caseId())).isEqualTo("ANALYZING");

        JsonNode callback = callbackSuccess(task, fixture);
        assertThat(callback.path("data").path("taskStatusCode").asText()).isEqualTo("SUCCESS");
        assertThat(callbackIsIdempotent(callback)).isFalse();
        assertThat(queryString("SELECT task_status_code FROM ana_task_record WHERE task_no = ?", task.taskNo())).isEqualTo("SUCCESS");
        assertThat(count("SELECT COUNT(1) FROM ana_result_summary WHERE task_id = ?", task.taskId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(1) FROM ana_visual_asset WHERE task_id = ?", task.taskId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(1) FROM med_risk_assessment_record WHERE case_id = ?", fixture.caseId())).isEqualTo(1);
        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", fixture.caseId())).isEqualTo("REVIEW_PENDING");
        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'ANALYZING'
                  AND to_status_code = 'REVIEW_PENDING'
                  AND change_reason_code = 'AI_CALLBACK_SUCCESS'
                """, fixture.caseId())).isEqualTo(1);

        long reportId = generateDoctorReport(fixture, "doctor confirmed");
        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", fixture.caseId())).isEqualTo("REPORT_READY");
        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'REVIEW_PENDING'
                  AND to_status_code = 'REPORT_READY'
                  AND change_reason_code = 'DOCTOR_CONFIRMED'
                """, fixture.caseId())).isEqualTo(1);
        var reportRow = queryRow("""
                SELECT report_type_code, version_no, report_status_code, attachment_id
                FROM rpt_record
                WHERE id = ?
                """, reportId);
        assertThat(reportRow.get("report_type_code")).isEqualTo("DOCTOR");
        assertThat(((Number) reportRow.get("version_no")).intValue()).isEqualTo(1);
        assertThat(reportRow.get("report_status_code")).isEqualTo("FINAL");
        assertThat(((Number) reportRow.get("attachment_id")).longValue()).isPositive();

        JsonNode export = exportReport(reportId);
        assertThat(export.path("data").path("exported").asBoolean()).isTrue();
        var exportLog = queryRow("""
                SELECT report_id, export_type_code, export_channel_code, exported_by, exported_at, org_id
                FROM rpt_export_log
                WHERE report_id = ?
                ORDER BY exported_at DESC
                LIMIT 1
                """, reportId);
        assertThat(((Number) exportLog.get("report_id")).longValue()).isEqualTo(reportId);
        assertThat(exportLog.get("export_type_code")).isEqualTo("PDF");
        assertThat(exportLog.get("export_channel_code")).isEqualTo("DOWNLOAD");
        assertThat(((Number) exportLog.get("exported_by")).longValue()).isPositive();
        assertThat(exportLog.get("exported_at")).isNotNull();
        assertThat(((Number) exportLog.get("org_id")).longValue()).isEqualTo(fixture.orgId());
    }

    @Test
    void shouldRejectReportGenerationBeforeReviewPending() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");

        JsonNode response = attemptGenerateReportBeforeReady(fixture);
        assertThat(response.path("code").asText()).isEqualTo("B9999");
        assertThat(count("SELECT COUNT(1) FROM rpt_record WHERE case_id = ?", fixture.caseId())).isEqualTo(0);
        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", fixture.caseId())).isEqualTo("QC_PENDING");
    }
}
