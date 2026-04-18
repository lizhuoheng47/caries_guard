package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CorrectionFeedbackGovernanceIntegrationTest extends AnalysisReportE2EBaseTest {

    @Test
    void shouldReviewExportAndAggregateCorrectionFeedback() throws Exception {
        TestFixture fixture = createFixture("REVIEW_PENDING");
        JsonNode statsBefore = getJson("/api/v1/dashboard/correction-feedback", fixture).path("data");

        ObjectNode originalInference = objectMapper.createObjectNode();
        originalInference.put("gradingLabel", "C2");
        originalInference.put("uncertaintyScore", 0.42);
        ObjectNode correctedTruth = objectMapper.createObjectNode();
        correctedTruth.put("gradingLabel", "C1");
        Map<String, Object> submitRequest = new LinkedHashMap<>();
        submitRequest.put("caseId", fixture.caseId());
        submitRequest.put("sourceImageId", fixture.imageId());
        submitRequest.put("feedbackTypeCode", "RE_GRADE");
        submitRequest.put("originalInferenceJson", originalInference);
        submitRequest.put("correctedTruthJson", correctedTruth);
        submitRequest.put("originalAiGrade", "C2");
        submitRequest.put("doctorCorrectedGrade", "C1");
        submitRequest.put("originalUncertainty", 0.42);
        submitRequest.put("acceptedAiConclusion", false);
        submitRequest.put("correctionReason", "e2e governance correction");
        submitRequest.put("trainingCandidate", true);

        JsonNode submit = postJson(
                "/api/v1/cases/" + fixture.caseId() + "/corrections",
                submitRequest,
                Map.of(),
                200);
        assertThat(submit.path("code").asText()).isEqualTo("00000");
        long feedbackId = submit.path("data").path("feedbackId").asLong();

        JsonNode review = postJson(
                "/api/v1/analysis/corrections/review",
                Map.of(
                        "feedbackIds", java.util.List.of(feedbackId),
                        "reviewStatusCode", "APPROVED",
                        "trainingCandidate", true),
                Map.of(),
                200);
        assertThat(review.path("code").asText()).isEqualTo("00000");
        assertThat(review.path("data").path("reviewedCount").asInt()).isEqualTo(1);
        assertThat(queryString("SELECT review_status_code FROM ana_correction_feedback WHERE id = ?", feedbackId))
                .isEqualTo("APPROVED");

        JsonNode export = postJson(
                "/api/v1/analysis/corrections/training-candidates/export",
                Map.of("limit", 500),
                Map.of(),
                200);
        assertThat(export.path("code").asText()).isEqualTo("00000");
        JsonNode exportData = export.path("data");
        assertThat(exportData.path("snapshotNo").asText()).startsWith("FBDS-");
        assertThat(exportData.path("samples")).anySatisfy(sample -> {
            assertThat(sample.path("feedbackId").asLong()).isEqualTo(feedbackId);
            assertThat(sample.path("originalAiGrade").asText()).isEqualTo("C2");
            assertThat(sample.path("doctorCorrectedGrade").asText()).isEqualTo("C1");
            assertThat(sample.path("acceptedAiConclusion").asBoolean()).isFalse();
        });

        Map<String, Object> feedbackRow = queryRow("""
                SELECT exported_snapshot_no, dataset_snapshot_no, desensitized_export_flag, is_exported_for_train
                FROM ana_correction_feedback
                WHERE id = ?
                """, feedbackId);
        assertThat(feedbackRow.get("exported_snapshot_no")).isNotNull();
        assertThat(feedbackRow.get("dataset_snapshot_no")).isEqualTo(feedbackRow.get("exported_snapshot_no"));
        assertThat(feedbackRow.get("desensitized_export_flag")).isEqualTo("1");
        assertThat(feedbackRow.get("is_exported_for_train")).isEqualTo("1");

        JsonNode statsAfter = getJson("/api/v1/dashboard/correction-feedback", fixture).path("data");
        assertThat(statsAfter.path("totalFeedbackCount").asLong())
                .isGreaterThanOrEqualTo(statsBefore.path("totalFeedbackCount").asLong() + 1);
        assertThat(statsAfter.path("approvedReviewCount").asLong())
                .isGreaterThanOrEqualTo(statsBefore.path("approvedReviewCount").asLong() + 1);
        assertThat(statsAfter.path("exportedSampleCount").asLong())
                .isGreaterThanOrEqualTo(statsBefore.path("exportedSampleCount").asLong() + 1);
        assertThat(statsAfter.path("aiCorrectedCount").asLong())
                .isGreaterThanOrEqualTo(statsBefore.path("aiCorrectedCount").asLong() + 1);
    }
}
