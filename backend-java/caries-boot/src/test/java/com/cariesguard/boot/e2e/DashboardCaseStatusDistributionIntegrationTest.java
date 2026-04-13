package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class DashboardCaseStatusDistributionIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldCountEachCaseIntoExactlyOneCurrentStatus() throws Exception {
        TestFixture fixture = createFixture("CREATED");
        createFixture("QC_PENDING");
        createFixture("ANALYZING");
        createFixture("REVIEW_PENDING");
        createFixture("REPORT_READY");
        createFixture("FOLLOWUP_REQUIRED");
        createFixture("CLOSED");
        createFixture("CANCELLED");

        JsonNode data = getJson("/api/v1/dashboard/case-status-distribution", fixture).path("data");

        assertThat(data.path("createdCount").asLong()).isEqualTo(1);
        assertThat(data.path("qcPendingCount").asLong()).isEqualTo(1);
        assertThat(data.path("analyzingCount").asLong()).isEqualTo(1);
        assertThat(data.path("reviewPendingCount").asLong()).isEqualTo(1);
        assertThat(data.path("reportReadyCount").asLong()).isEqualTo(1);
        assertThat(data.path("followupRequiredCount").asLong()).isEqualTo(1);
        assertThat(data.path("closedCount").asLong()).isEqualTo(1);
        assertThat(data.path("cancelledCount").asLong()).isEqualTo(1);
        assertThat(
                data.path("createdCount").asLong()
                        + data.path("qcPendingCount").asLong()
                        + data.path("analyzingCount").asLong()
                        + data.path("reviewPendingCount").asLong()
                        + data.path("reportReadyCount").asLong()
                        + data.path("followupRequiredCount").asLong()
                        + data.path("closedCount").asLong()
                        + data.path("cancelledCount").asLong()
        ).isEqualTo(8);
    }
}
