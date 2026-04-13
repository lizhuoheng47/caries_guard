package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DashboardTrendIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldReturnZeroFilledTrendPointsForCustomRange() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDate endDate = LocalDate.now();
        LocalDate day1 = startDate;
        LocalDate day3 = startDate.plusDays(2);
        LocalDate day5 = startDate.plusDays(4);

        TestFixture caseOnDay1 = createFixture("QC_PENDING");
        TestFixture caseOnDay3 = createFixture("QC_PENDING");
        TestFixture caseOutOfRange = createFixture("QC_PENDING");

        updateCaseCreatedAt(caseOnDay1.caseId(), day1.atTime(9, 0));
        updateCaseCreatedAt(caseOnDay3.caseId(), day3.atTime(10, 0));
        updateCaseCreatedAt(caseOutOfRange.caseId(), startDate.minusDays(1).atTime(8, 0));

        insertSuccessfulAnalysis(caseOnDay1.caseId(), caseOnDay1.patientId(), caseOnDay1.orgId(), day3.atTime(14, 0));
        insertSuccessfulAnalysis(caseOnDay3.caseId(), caseOnDay3.patientId(), caseOnDay3.orgId(), day5.atTime(11, 0));
        insertSuccessfulAnalysis(caseOutOfRange.caseId(), caseOutOfRange.patientId(), caseOutOfRange.orgId(), startDate.minusDays(1).atTime(11, 0));

        insertReport(caseOnDay1.caseId(), caseOnDay1.patientId(), caseOnDay1.orgId(), day5.atTime(15, 0));
        insertPlan(caseOnDay3.caseId(), caseOnDay3.patientId(), caseOnDay3.orgId(), day5.atTime(16, 0));

        JsonNode data = getJson(
                "/api/v1/dashboard/trend?rangeType=CUSTOM&startDate=" + startDate + "&endDate=" + endDate,
                caseOnDay1).path("data");

        assertThat(data).hasSize(7);
        assertPoint(data.get(0), day1, 1, 0, 0, 0);
        assertPoint(data.get(1), startDate.plusDays(1), 0, 0, 0, 0);
        assertPoint(data.get(2), day3, 1, 1, 0, 0);
        assertPoint(data.get(3), startDate.plusDays(3), 0, 0, 0, 0);
        assertPoint(data.get(4), day5, 0, 1, 1, 1);
        assertPoint(data.get(5), startDate.plusDays(5), 0, 0, 0, 0);
        assertPoint(data.get(6), endDate, 0, 0, 0, 0);
    }

    private void assertPoint(JsonNode point,
                             LocalDate expectedDate,
                             long newCaseCount,
                             long analysisCompletedCount,
                             long reportGeneratedCount,
                             long followupTriggeredCount) {
        assertThat(point.path("date").asText()).isEqualTo(expectedDate.toString());
        assertThat(point.path("newCaseCount").asLong()).isEqualTo(newCaseCount);
        assertThat(point.path("analysisCompletedCount").asLong()).isEqualTo(analysisCompletedCount);
        assertThat(point.path("reportGeneratedCount").asLong()).isEqualTo(reportGeneratedCount);
        assertThat(point.path("followupTriggeredCount").asLong()).isEqualTo(followupTriggeredCount);
    }

    private void updateCaseCreatedAt(Long caseId, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                UPDATE med_case
                SET created_at = ?, updated_at = ?
                WHERE id = ?
                """,
                createdAt, createdAt, caseId);
    }

    private void insertSuccessfulAnalysis(Long caseId, Long patientId, Long orgId, LocalDateTime completedAt) {
        long taskId = nextId();
        jdbcTemplate.update("""
                INSERT INTO ana_task_record
                (id, task_no, case_id, patient_id, model_version, task_type_code, task_status_code,
                 request_payload_json, started_at, completed_at, org_id, status, deleted_flag, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'caries-v1', 'INFERENCE', 'SUCCESS',
                        JSON_OBJECT('source', 'dashboard-trend'), ?, ?, ?, 'ACTIVE', 0, ?, ?, ?)
                """,
                taskId,
                "TASK" + taskId,
                caseId,
                patientId,
                completedAt,
                completedAt,
                orgId,
                100001L,
                completedAt,
                completedAt);
    }

    private void insertReport(Long caseId, Long patientId, Long orgId, LocalDateTime createdAt) {
        long reportId = nextId();
        jdbcTemplate.update("""
                INSERT INTO rpt_record
                (id, report_no, case_id, patient_id, attachment_id, report_type_code, report_status_code, version_no,
                 summary_text, generated_at, org_id, status, deleted_flag, created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, ?, ?, NULL, 'DOCTOR', 'FINAL', 1, 'dashboard trend report', ?, ?, 'ACTIVE', 0, ?, ?, ?, ?)
                """,
                reportId,
                "RPT" + reportId,
                caseId,
                patientId,
                createdAt,
                orgId,
                100001L,
                createdAt,
                100001L,
                createdAt);
    }

    private void insertPlan(Long caseId, Long patientId, Long orgId, LocalDateTime createdAt) {
        long planId = nextId();
        jdbcTemplate.update("""
                INSERT INTO fup_plan
                (id, plan_no, case_id, patient_id, plan_type_code, plan_status_code, next_followup_date, interval_days,
                 owner_user_id, org_id, status, deleted_flag, remark, trigger_source_code, trigger_ref_id,
                 created_by, created_at, updated_by, updated_at)
                VALUES (?, ?, ?, ?, 'HIGH_RISK', 'ACTIVE', ?, 30, ?, ?, 'ACTIVE', 0, 'dashboard trend plan',
                        'RISK_HIGH', ?, ?, ?, ?, ?)
                """,
                planId,
                "FUP" + planId,
                caseId,
                patientId,
                createdAt.toLocalDate().plusDays(7),
                100001L,
                orgId,
                planId,
                100001L,
                createdAt,
                100001L,
                createdAt);
    }
}
