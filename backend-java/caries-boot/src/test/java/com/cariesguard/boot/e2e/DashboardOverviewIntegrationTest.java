package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class DashboardOverviewIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldAggregateOverviewByFrozenDefinition() throws Exception {
        TestFixture followupFixture = createFixture("QC_PENDING");
        AnalysisTaskRef task = createAnalysisTask(followupFixture, "dashboard overview");
        callbackSuccess(task, followupFixture);
        generateDoctorReport(followupFixture, "dashboard overview report");

        createFixture("CLOSED");
        createFixture("QC_PENDING");

        JsonNode response = getJson("/api/v1/dashboard/overview", followupFixture);
        JsonNode data = response.path("data");

        assertThat(data.path("patientCount").asLong()).isEqualTo(3);
        assertThat(data.path("caseCount").asLong()).isEqualTo(3);
        assertThat(data.path("analyzedCaseCount").asLong()).isEqualTo(1);
        assertThat(data.path("generatedReportCount").asLong()).isEqualTo(1);
        assertThat(data.path("followupRequiredCaseCount").asLong()).isEqualTo(1);
        assertThat(data.path("closedCaseCount").asLong()).isEqualTo(1);
    }
}
