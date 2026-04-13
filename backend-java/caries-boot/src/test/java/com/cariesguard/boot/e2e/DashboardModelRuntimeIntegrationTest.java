package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DashboardModelRuntimeIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldCountRecentTasksAndResolveLatestSuccessfulModelVersion() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");

        insertTaskRecord(fixture.caseId(), fixture.patientId(), fixture.orgId(), "SUCCESS", "caries-v1", LocalDateTime.now().minusDays(29), LocalDateTime.now().minusDays(29));
        insertTaskRecord(fixture.caseId(), fixture.patientId(), fixture.orgId(), "FAILED", "caries-v1", LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(3));
        insertTaskRecord(fixture.caseId(), fixture.patientId(), fixture.orgId(), "PROCESSING", "caries-v2", LocalDateTime.now().minusDays(1), null);
        insertTaskRecord(fixture.caseId(), fixture.patientId(), fixture.orgId(), "SUCCESS", "caries-v2", LocalDateTime.now(), LocalDateTime.now());
        insertTaskRecord(fixture.caseId(), fixture.patientId(), fixture.orgId(), "SUCCESS", "caries-old", LocalDateTime.now().minusDays(40), LocalDateTime.now().minusDays(40));

        JsonNode data = getJson("/api/v1/dashboard/model-runtime", fixture).path("data");

        assertThat(data.path("currentModelVersion").asText()).isEqualTo("caries-v2");
        assertThat(data.path("recentTaskCount").asLong()).isEqualTo(4);
        assertThat(data.path("successTaskCount").asLong()).isEqualTo(2);
        assertThat(data.path("failedTaskCount").asLong()).isEqualTo(1);
        assertThat(data.path("successRate").decimalValue()).isEqualByComparingTo("0.5000");
    }

    private void insertTaskRecord(Long caseId,
                                  Long patientId,
                                  Long orgId,
                                  String statusCode,
                                  String modelVersion,
                                  LocalDateTime createdAt,
                                  LocalDateTime completedAt) {
        long taskId = nextId();
        jdbcTemplate.update("""
                INSERT INTO ana_task_record
                (id, task_no, case_id, patient_id, model_version, task_type_code, task_status_code,
                 request_payload_json, started_at, completed_at, org_id, status, deleted_flag, created_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'INFERENCE', ?, JSON_OBJECT('source', 'dashboard-test'), ?, ?, ?, 'ACTIVE', 0, ?, ?, ?)
                """,
                taskId,
                "TASK" + taskId,
                caseId,
                patientId,
                modelVersion,
                statusCode,
                createdAt,
                completedAt,
                orgId,
                100001L,
                createdAt,
                createdAt);
    }
}
