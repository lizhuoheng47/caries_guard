package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class FollowupOverdueIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldMarkTaskOverdueAndWriteAlertNotifyRecord() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");
        AnalysisTaskRef task = createAnalysisTask(fixture, "e2e followup overdue");

        callbackSuccess(task, fixture);
        generateDoctorReport(fixture, "followup overdue confirmed");

        long taskId = ((Number) queryRow("""
                SELECT id
                FROM fup_task
                WHERE case_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """, fixture.caseId()).get("id")).longValue();

        JsonNode response = updateFollowupTaskStatus(fixture, taskId, "OVERDUE", "task overdue");
        assertThat(response.path("data").path("taskStatusCode").asText()).isEqualTo("OVERDUE");
        assertThat(queryString("SELECT task_status_code FROM fup_task WHERE id = ?", taskId)).isEqualTo("OVERDUE");
        assertThat(queryRow("SELECT completed_at FROM fup_task WHERE id = ?", taskId).get("completed_at")).isNull();

        assertThat(count("""
                SELECT COUNT(1)
                FROM msg_notify_record
                WHERE biz_module_code = 'FOLLOWUP'
                  AND biz_id = ?
                  AND notify_type_code = 'ALERT'
                """, taskId)).isEqualTo(1);
        var notifyRow = queryRow("""
                SELECT channel_code, send_status_code, org_id
                FROM msg_notify_record
                WHERE biz_module_code = 'FOLLOWUP'
                  AND biz_id = ?
                  AND notify_type_code = 'ALERT'
                LIMIT 1
                """, taskId);
        assertThat(notifyRow.get("channel_code")).isEqualTo("IN_APP");
        assertThat(notifyRow.get("send_status_code")).isEqualTo("PENDING");
        assertThat(((Number) notifyRow.get("org_id")).longValue()).isEqualTo(fixture.orgId());
    }
}
