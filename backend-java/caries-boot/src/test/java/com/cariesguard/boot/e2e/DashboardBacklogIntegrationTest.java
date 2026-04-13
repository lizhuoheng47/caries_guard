package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DashboardBacklogIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldCountPendingReviewTodoOverdueAndOpenHighRiskCases() throws Exception {
        TestFixture reviewFixture = createFixture("REVIEW_PENDING");
        TestFixture followupFixture = createFixture("FOLLOWUP_REQUIRED");
        TestFixture closedFixture = createFixture("CLOSED");

        insertRiskRecord(reviewFixture.caseId(), reviewFixture.patientId(), reviewFixture.orgId(), "HIGH");
        insertRiskRecord(followupFixture.caseId(), followupFixture.patientId(), followupFixture.orgId(), "HIGH");
        insertRiskRecord(closedFixture.caseId(), closedFixture.patientId(), closedFixture.orgId(), "HIGH");

        long planId = insertPlan(followupFixture.caseId(), followupFixture.patientId(), followupFixture.orgId());
        insertTask(planId, followupFixture.caseId(), followupFixture.patientId(), followupFixture.orgId(), "TODO");
        insertTask(planId, followupFixture.caseId(), followupFixture.patientId(), followupFixture.orgId(), "IN_PROGRESS");
        insertTask(planId, followupFixture.caseId(), followupFixture.patientId(), followupFixture.orgId(), "OVERDUE");

        JsonNode data = getJson("/api/v1/dashboard/backlog-summary", reviewFixture).path("data");

        assertThat(data.path("reviewPendingCaseCount").asLong()).isEqualTo(1);
        assertThat(data.path("todoFollowupTaskCount").asLong()).isEqualTo(2);
        assertThat(data.path("overdueFollowupTaskCount").asLong()).isEqualTo(1);
        assertThat(data.path("highRiskPendingCaseCount").asLong()).isEqualTo(2);
    }

    private void insertRiskRecord(Long caseId, Long patientId, Long orgId, String level) {
        jdbcTemplate.update("""
                INSERT INTO med_risk_assessment_record
                (id, case_id, patient_id, overall_risk_level_code, assessment_report_json, recommended_cycle_days,
                 assessed_at, org_id, status, deleted_flag, created_by, updated_by)
                VALUES (?, ?, ?, ?, JSON_OBJECT('level', ?), 30, ?, ?, 'ACTIVE', 0, ?, ?)
                """,
                nextId(), caseId, patientId, level, level, LocalDateTime.now(), orgId, 100001L, 100001L);
    }

    private long insertPlan(Long caseId, Long patientId, Long orgId) {
        long planId = nextId();
        jdbcTemplate.update("""
                INSERT INTO fup_plan
                (id, plan_no, case_id, patient_id, plan_type_code, plan_status_code, next_followup_date, interval_days,
                 owner_user_id, org_id, status, deleted_flag, remark, trigger_source_code, trigger_ref_id, created_by, updated_by)
                VALUES (?, ?, ?, ?, 'HIGH_RISK', 'ACTIVE', ?, 30, ?, ?, 'ACTIVE', 0, 'backlog plan', 'RISK_HIGH', ?, ?, ?)
                """,
                planId, "FUP" + planId, caseId, patientId, LocalDate.now().plusDays(5), 100001L, orgId, planId, 100001L, 100001L);
        return planId;
    }

    private void insertTask(Long planId, Long caseId, Long patientId, Long orgId, String statusCode) {
        long taskId = nextId();
        jdbcTemplate.update("""
                INSERT INTO fup_task
                (id, task_no, plan_id, case_id, patient_id, task_type_code, task_status_code, assigned_to_user_id,
                 due_date, org_id, status, deleted_flag, remark, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, 'FOLLOW_CONTACT', ?, ?, ?, ?, 'ACTIVE', 0, 'backlog task', ?, ?)
                """,
                taskId, "TSK" + taskId, planId, caseId, patientId, statusCode, 100001L,
                LocalDate.now().plusDays(2), orgId, 100001L, 100001L);
    }
}
