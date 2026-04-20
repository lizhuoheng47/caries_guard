package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class AnalysisReviewSemanticsE2ETest extends AnalysisReportE2EBaseTest {

    @Test
    void highUncertaintyCallbackShouldBeConsumableByAnalysisAndReviewViews() throws Exception {
        TestFixture fixture = createFixture("QC_PENDING");
        AnalysisTaskRef task = createAnalysisTask(fixture, "e2e review semantics");

        JsonNode callback = callbackSuccess(task, fixture);
        assertThat(callback.path("data").path("taskStatusCode").asText()).isEqualTo("SUCCESS");
        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", fixture.caseId())).isEqualTo("REVIEW_PENDING");

        JsonNode analysisDetail = getJson("/api/v1/analysis/tasks/" + task.taskId(), fixture).path("data");
        JsonNode summary = analysisDetail.path("summary");
        assertThat(summary.path("gradingLabel").asText()).isEqualTo("C3");
        assertThat(summary.path("uncertaintyScore").asDouble()).isGreaterThanOrEqualTo(0.62d);
        assertThat(summary.path("needsReview").asBoolean()).isTrue();
        assertThat(summary.path("rawResultJson").path("gradingLabel").asText()).isEqualTo("C3");
        assertThat(summary.path("rawResultJson").path("needsReview").asBoolean()).isTrue();

        JsonNode queueRecords = getJson("/api/v1/review/queue?pageNo=1&pageSize=50", fixture)
                .path("data")
                .path("records");
        JsonNode queueItem = findByTaskNo(queueRecords, task.taskNo());
        assertThat(queueItem).isNotNull();
        assertThat(queueItem.path("reviewStatusCode").asText()).isEqualTo("REVIEW_PENDING");
        assertThat(queueItem.path("gradingLabel").asText()).isEqualTo("C3");
        assertThat(queueItem.path("uncertaintyScore").asDouble()).isGreaterThanOrEqualTo(0.62d);
        assertThat(queueItem.path("needsReview").asBoolean()).isTrue();

        JsonNode reviewDetail = getJson("/api/v1/review/tasks/" + task.taskNo(), fixture).path("data");
        assertThat(reviewDetail.path("reviewStatusCode").asText()).isEqualTo("REVIEW_PENDING");
        JsonNode aiResult = reviewDetail.path("aiResult");
        assertThat(aiResult.path("gradingLabel").asText()).isEqualTo("C3");
        assertThat(aiResult.path("uncertaintyScore").asDouble()).isGreaterThanOrEqualTo(0.62d);
        assertThat(aiResult.path("needsReview").asBoolean()).isTrue();
    }

    private JsonNode findByTaskNo(JsonNode records, String taskNo) {
        if (records == null || !records.isArray()) {
            return null;
        }
        for (JsonNode item : records) {
            if (taskNo.equals(item.path("taskNo").asText())) {
                return item;
            }
        }
        return null;
    }
}
