package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnalysisReportFollowupE2ETest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldPersistFollowupPlanTaskNotifyAndStatusLogsAfterHighRiskReport() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");
        AnalysisTaskRef task = createAnalysisTask(fixture, "e2e followup");

        callbackSuccess(task, fixture);
        long reportId = generateDoctorReport(fixture, "high risk confirmed");

        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", fixture.caseId())).isEqualTo("FOLLOWUP_REQUIRED");
        assertThat(queryString("SELECT followup_required_flag FROM med_case WHERE id = ?", fixture.caseId())).isEqualTo("1");

        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'REVIEW_PENDING'
                  AND to_status_code = 'REPORT_READY'
                  AND change_reason_code = 'DOCTOR_CONFIRMED'
                """, fixture.caseId())).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'REPORT_READY'
                  AND to_status_code = 'FOLLOWUP_REQUIRED'
                  AND change_reason_code = 'FOLLOWUP_TRIGGERED'
                """, fixture.caseId())).isEqualTo(1);

        assertThat(count("SELECT COUNT(1) FROM fup_plan WHERE case_id = ?", fixture.caseId())).isEqualTo(1);
        var planRow = queryRow("""
                SELECT id, plan_status_code, trigger_source_code, trigger_ref_id
                FROM fup_plan
                WHERE case_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """, fixture.caseId());
        long planId = ((Number) planRow.get("id")).longValue();
        assertThat(planRow.get("plan_status_code")).isEqualTo("ACTIVE");
        assertThat(planRow.get("trigger_source_code")).isEqualTo("RISK_HIGH");
        assertThat(((Number) planRow.get("trigger_ref_id")).longValue()).isEqualTo(reportId);

        assertThat(count("SELECT COUNT(1) FROM fup_task WHERE plan_id = ?", planId)).isEqualTo(1);
        var taskRow = queryRow("""
                SELECT id, task_type_code, task_status_code, due_date
                FROM fup_task
                WHERE plan_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """, planId);
        long followupTaskId = ((Number) taskRow.get("id")).longValue();
        assertThat(taskRow.get("task_type_code")).isEqualTo("FOLLOW_CONTACT");
        assertThat(taskRow.get("task_status_code")).isEqualTo("TODO");
        assertThat(taskRow.get("due_date")).isNotNull();

        assertThat(count("""
                SELECT COUNT(1)
                FROM msg_notify_record
                WHERE biz_module_code = 'FOLLOWUP' AND biz_id = ?
                """, followupTaskId)).isGreaterThanOrEqualTo(1);
        var notifyRow = queryRow("""
                SELECT notify_type_code, channel_code, send_status_code, org_id
                FROM msg_notify_record
                WHERE biz_module_code = 'FOLLOWUP' AND biz_id = ?
                ORDER BY created_at DESC
                LIMIT 1
                """, followupTaskId);
        assertThat(notifyRow.get("notify_type_code")).isEqualTo("REMINDER");
        assertThat(notifyRow.get("channel_code")).isEqualTo("IN_APP");
        assertThat(notifyRow.get("send_status_code")).isEqualTo("PENDING");
        assertThat(((Number) notifyRow.get("org_id")).longValue()).isEqualTo(fixture.orgId());
    }
}
