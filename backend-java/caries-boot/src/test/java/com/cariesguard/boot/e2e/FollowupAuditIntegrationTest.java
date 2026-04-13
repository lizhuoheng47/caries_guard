package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class FollowupAuditIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldPersistRecordCompleteTaskAndClosePlanAfterFollowupExecution() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");
        AnalysisTaskRef task = createAnalysisTask(fixture, "e2e followup audit");

        callbackSuccess(task, fixture);
        generateDoctorReport(fixture, "followup audit confirmed");

        var taskRow = queryRow("""
                SELECT t.id, t.plan_id
                FROM fup_task t
                WHERE t.case_id = ?
                ORDER BY t.created_at DESC
                LIMIT 1
                """, fixture.caseId());
        long taskId = ((Number) taskRow.get("id")).longValue();
        long planId = ((Number) taskRow.get("plan_id")).longValue();

        JsonNode recordResponse = addFollowupRecord(fixture, taskId, false, null, "patient stable");
        long recordId = recordResponse.path("data").path("recordId").asLong();
        assertThat(recordId).isPositive();

        var recordRow = queryRow("""
                SELECT task_id, plan_id, followup_method_code, contact_result_code, follow_next_flag, outcome_summary
                FROM fup_record
                WHERE id = ?
                """, recordId);
        assertThat(((Number) recordRow.get("task_id")).longValue()).isEqualTo(taskId);
        assertThat(((Number) recordRow.get("plan_id")).longValue()).isEqualTo(planId);
        assertThat(recordRow.get("followup_method_code")).isEqualTo("PHONE");
        assertThat(recordRow.get("contact_result_code")).isEqualTo("REACHED");
        assertThat(recordRow.get("follow_next_flag")).isEqualTo("0");
        assertThat(recordRow.get("outcome_summary")).isEqualTo("patient stable");

        assertThat(queryString("SELECT task_status_code FROM fup_task WHERE id = ?", taskId)).isEqualTo("DONE");
        assertThat(queryRow("SELECT completed_at FROM fup_task WHERE id = ?", taskId).get("completed_at")).isNotNull();
        assertThat(queryString("SELECT plan_status_code FROM fup_plan WHERE id = ?", planId)).isEqualTo("DONE");

        assertThat(count("SELECT COUNT(1) FROM fup_task WHERE plan_id = ?", planId)).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(1)
                FROM msg_notify_record
                WHERE biz_module_code = 'FOLLOWUP'
                  AND biz_id = ?
                """, taskId)).isGreaterThanOrEqualTo(1);
    }
}
