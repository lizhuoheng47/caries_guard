package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DashboardRiskLevelDistributionIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldUseLatestRiskRecordPerCase() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");
        TestFixture secondFixture = createFixture("QC_PENDING");
        TestFixture thirdFixture = createFixture("QC_PENDING");

        insertRiskRecord(fixture.caseId(), fixture.patientId(), fixture.orgId(), "HIGH", LocalDateTime.now().minusDays(2));
        insertRiskRecord(fixture.caseId(), fixture.patientId(), fixture.orgId(), "LOW", LocalDateTime.now().minusDays(1));
        insertRiskRecord(secondFixture.caseId(), secondFixture.patientId(), secondFixture.orgId(), "MEDIUM", LocalDateTime.now());
        insertRiskRecord(thirdFixture.caseId(), thirdFixture.patientId(), thirdFixture.orgId(), "HIGH", LocalDateTime.now());

        JsonNode data = getJson("/api/v1/dashboard/risk-level-distribution", fixture).path("data");

        assertThat(data.path("highRiskCount").asLong()).isEqualTo(1);
        assertThat(data.path("mediumRiskCount").asLong()).isEqualTo(1);
        assertThat(data.path("lowRiskCount").asLong()).isEqualTo(1);
    }

    private void insertRiskRecord(Long caseId, Long patientId, Long orgId, String level, LocalDateTime assessedAt) {
        jdbcTemplate.update("""
                INSERT INTO med_risk_assessment_record
                (id, case_id, patient_id, overall_risk_level_code, assessment_report_json, recommended_cycle_days,
                 assessed_at, org_id, status, deleted_flag, created_by, updated_by)
                VALUES (?, ?, ?, ?, JSON_OBJECT('level', ?), 30, ?, ?, 'ACTIVE', 0, ?, ?)
                """,
                nextId(), caseId, patientId, level, level, assessedAt, orgId, 100001L, 100001L);
    }
}
