package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ModelGovernanceIntegrationTest extends AnalysisReportE2EBaseTest {

    private Long modelVersionId;
    private Long datasetSnapshotId;
    private Long evaluationId;
    private String modelVersion;
    private String datasetVersion;

    @AfterEach
    void cleanupGovernanceData() {
        try {
            if (evaluationId != null) {
                jdbcTemplate.update("DELETE FROM ana_model_eval_record WHERE id = ?", evaluationId);
            }
            if (datasetSnapshotId != null) {
                jdbcTemplate.update("DELETE FROM trn_dataset_sample WHERE snapshot_id = ?", datasetSnapshotId);
                jdbcTemplate.update("DELETE FROM trn_dataset_snapshot WHERE id = ?", datasetSnapshotId);
            } else if (datasetVersion != null) {
                jdbcTemplate.update("DELETE FROM trn_dataset_snapshot WHERE dataset_version = ?", datasetVersion);
            }
            if (modelVersionId != null) {
                jdbcTemplate.update("DELETE FROM ana_model_version_registry WHERE id = ?", modelVersionId);
            } else if (modelVersion != null) {
                jdbcTemplate.update("DELETE FROM ana_model_version_registry WHERE model_version = ?", modelVersion);
            }
        } catch (Exception ignored) {
            // Keep cleanup best-effort for e2e isolation.
        }
    }

    @Test
    void shouldRegisterApproveCreateEvalDatasetAndRecordEvaluation() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        modelVersion = "caries-eval-" + suffix;
        datasetVersion = "eval-dataset-" + suffix;
        authenticateAsSysAdmin(100001L);

        JsonNode registered = postJson(
                "/api/v1/analysis/model-governance/model-versions",
                Map.of(
                        "modelCode", "caries-detector",
                        "modelVersion", modelVersion,
                        "modelTypeCode", "DETECTION",
                        "remark", "e2e candidate"),
                Map.of(),
                200).path("data");
        modelVersionId = registered.path("modelVersionId").asLong();
        assertThat(registered.path("status").asText()).isEqualTo("CANDIDATE");

        JsonNode approved = postJson(
                "/api/v1/analysis/model-governance/model-versions/" + modelVersionId + "/approval",
                Map.of("decisionCode", "APPROVED", "remark", "passed e2e eval"),
                Map.of(),
                200).path("data");
        assertThat(approved.path("approvedFlag").asText()).isEqualTo("1");
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");

        Map<String, Object> snapshotRequest = new LinkedHashMap<>();
        snapshotRequest.put("datasetVersion", datasetVersion);
        snapshotRequest.put("snapshotTypeCode", "EVAL");
        snapshotRequest.put("sourceSummary", "e2e approved correction snapshot");
        snapshotRequest.put("metadataJson", Map.of("source", "e2e", "governance", true));
        snapshotRequest.put("datasetCardPath", "datasets/" + datasetVersion + "/card.md");
        snapshotRequest.put("samples", List.of(Map.of(
                "sampleRefNo", "FB-E2E-" + suffix,
                "patientUuid", "patient-e2e",
                "imageRefNo", "image-e2e",
                "sourceTypeCode", "CORRECTION",
                "splitTypeCode", "EVAL",
                "labelVersion", "label-v1",
                "labelJson", Map.of("doctorCorrectedGrade", "C1"))));
        JsonNode snapshot = postJson(
                "/api/v1/analysis/model-governance/dataset-snapshots",
                snapshotRequest,
                Map.of(),
                200).path("data");
        datasetSnapshotId = snapshot.path("snapshotId").asLong();
        assertThat(snapshot.path("sampleCount").asInt()).isEqualTo(1);
        assertThat(snapshot.path("samples").get(0).path("labelJson").path("doctorCorrectedGrade").asText()).isEqualTo("C1");

        JsonNode evaluation = postJson(
                "/api/v1/analysis/model-governance/evaluations",
                Map.of(
                        "modelVersionId", modelVersionId,
                        "datasetSnapshotId", datasetSnapshotId,
                        "evalTypeCode", "OFFLINE",
                        "metricJson", Map.of("accuracy", 0.93, "recall", 0.88),
                        "errorCaseJson", List.of("FB-E2E-" + suffix),
                        "evidenceAttachmentKey", "eval/" + suffix + "/evidence.json"),
                Map.of(),
                200).path("data");
        evaluationId = evaluation.path("evaluationId").asLong();
        assertThat(evaluation.path("metricJson").path("accuracy").asDouble()).isEqualTo(0.93);

        assertThat(count("SELECT COUNT(1) FROM trn_dataset_sample WHERE snapshot_id = ?", datasetSnapshotId)).isEqualTo(1);
        Map<String, Object> evalRow = queryRow("SELECT metric_json, dataset_snapshot_id FROM ana_model_eval_record WHERE id = ?", evaluationId);
        assertThat(evalRow.get("metric_json").toString()).contains("accuracy");
        assertThat(((Number) evalRow.get("dataset_snapshot_id")).longValue()).isEqualTo(datasetSnapshotId);
    }
}
