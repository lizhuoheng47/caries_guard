package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DashboardFollowupTaskSummaryIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldApplyCompletionAndOverdueFormulasExcludingCancelled() throws Exception {
        TestFixture fixture = createFixture("FOLLOWUP_REQUIRED");
        long planId = insertPlan(fixture.caseId(), fixture.patientId(), fixture.orgId());

        insertTask(planId, fixture.caseId(), fixture.patientId(), fixture.orgId(), "TODO");
        insertTask(planId, fixture.caseId(), fixture.patientId(), fixture.orgId(), "IN_PROGRESS");
        insertTask(planId, fixture.caseId(), fixture.patientId(), fixture.orgId(), "DONE");
        insertTask(planId, fixture.caseId(), fixture.patientId(), fixture.orgId(), "OVERDUE");
        insertTask(planId, fixture.caseId(), fixture.patientId(), fixture.orgId(), "CANCELLED");

        JsonNode data = getJson("/api/v1/dashboard/followup-task-summary", fixture).path("data");

        assertThat(data.path("todoCount").asLong()).isEqualTo(1);
        assertThat(data.path("inProgressCount").asLong()).isEqualTo(1);
        assertThat(data.path("doneCount").asLong()).isEqualTo(1);
        assertThat(data.path("overdueCount").asLong()).isEqualTo(1);
        assertThat(data.path("completionRate").decimalValue()).isEqualByComparingTo("0.2500");
        assertThat(data.path("overdueRate").decimalValue()).isEqualByComparingTo("0.2500");
    }

    private long insertPlan(Long caseId, Long patientId, Long orgId) {
        long planId = nextId();
        jdbcTemplate.update("""
                INSERT INTO fup_plan
                (id, plan_no, case_id, patient_id, plan_type_code, plan_status_code, next_followup_date, interval_days,
                 owner_user_id, org_id, status, deleted_flag, remark, trigger_source_code, trigger_ref_id, created_by, updated_by)
                VALUES (?, ?, ?, ?, 'HIGH_RISK', 'ACTIVE', ?, 30, ?, ?, 'ACTIVE', 0, 'dashboard plan', 'RISK_HIGH', ?, ?, ?)
                """,
                planId, "FUP" + planId, caseId, patientId, LocalDate.now().plusDays(7), 100001L, orgId, planId, 100001L, 100001L);
        return planId;
    }

    private void insertTask(Long planId, Long caseId, Long patientId, Long orgId, String statusCode) {
        jdbcTemplate.update("""
                INSERT INTO fup_task
                (id, task_no, plan_id, case_id, patient_id, task_type_code, task_status_code, assigned_to_user_id,
                 due_date, org_id, status, deleted_flag, remark, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, 'FOLLOW_CONTACT', ?, ?, ?, ?, 'ACTIVE', 0, 'dashboard task', ?, ?)
                """,
                nextId(), "TSK" + nextId(), planId, caseId, patientId, statusCode, 100001L,
                LocalDate.now().plusDays(3), orgId, 100001L, 100001L);
    }
}
