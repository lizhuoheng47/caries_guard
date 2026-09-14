package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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

        Map<String, Object> reviewCommand = Map.of(
                "revisedGrade", "C2",
                "revisedDetections", List.of(Map.of(
                        "id", "doctor-1",
                        "x", 0.10,
                        "y", 0.20,
                        "width", 0.30,
                        "height", 0.25,
                        "label", "C2",
                        "confidence", 0.91)),
                "reasonTags", List.of("BOUNDARY_CASE"),
                "note", "E2E persisted review");
        MvcResult draftResult = mockMvc.perform(MockMvcRequestBuilders
                        .put("/api/v1/review/tasks/" + task.taskNo() + "/draft")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewCommand)))
                .andReturn();
        assertThat(draftResult.getResponse().getStatus()).isEqualTo(200);
        JsonNode draft = objectMapper.readTree(draftResult.getResponse().getContentAsString()).path("data");
        assertThat(draft.path("statusCode").asText()).isEqualTo("DRAFT");
        assertThat(draft.path("revisedGrade").asText()).isEqualTo("C2");

        JsonNode reloaded = getJson("/api/v1/review/tasks/" + task.taskNo(), fixture).path("data").path("doctorDraft");
        assertThat(reloaded.path("note").asText()).isEqualTo("E2E persisted review");
        assertThat(reloaded.path("revisedDetections").size()).isEqualTo(1);

        JsonNode submitted = postJson(
                "/api/v1/review/tasks/" + task.taskNo() + "/submit",
                reviewCommand,
                Map.of(),
                200).path("data");
        assertThat(submitted.path("doctorCorrectedGrade").asText()).isEqualTo("C2");
        assertThat(count("SELECT COUNT(1) FROM ana_review_draft WHERE task_id = ? AND draft_status_code = 'SUBMITTED'", task.taskId()))
                .isEqualTo(1L);
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
