package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class ReportExportAuditIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldWriteExportAuditLogAfterReportExport() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");
        AnalysisTaskRef task = createAnalysisTask(fixture, "e2e export audit");
        callbackSuccess(task, fixture);

        long reportId = generateDoctorReport(fixture, "audit check");
        JsonNode exportResponse = exportReportWithoutBody(reportId);
        assertThat(exportResponse.path("data").path("reportId").asLong()).isEqualTo(reportId);
        assertThat(exportResponse.path("data").path("exported").asBoolean()).isTrue();

        assertThat(count("SELECT COUNT(1) FROM rpt_export_log WHERE report_id = ?", reportId)).isEqualTo(1);
        var logRow = queryRow("""
                SELECT report_id, export_type_code, export_channel_code, exported_by, exported_at, org_id
                FROM rpt_export_log
                WHERE report_id = ?
                LIMIT 1
                """, reportId);
        assertThat(((Number) logRow.get("report_id")).longValue()).isEqualTo(reportId);
        assertThat(logRow.get("export_type_code")).isEqualTo("PDF");
        assertThat(logRow.get("export_channel_code")).isEqualTo("DOWNLOAD");
        assertThat(((Number) logRow.get("exported_by")).longValue()).isPositive();
        assertThat(logRow.get("exported_at")).isNotNull();
        assertThat(((Number) logRow.get("org_id")).longValue()).isEqualTo(fixture.orgId());
    }
}
