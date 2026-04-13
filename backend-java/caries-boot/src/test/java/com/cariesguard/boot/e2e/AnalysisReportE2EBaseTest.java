package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.cariesguard.analysis.config.AnalysisProperties;
import com.cariesguard.boot.CariesBootApplication;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.image.domain.model.StoredObject;
import com.cariesguard.image.domain.service.ObjectStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(classes = CariesBootApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("e2e")
abstract class AnalysisReportE2EBaseTest {

    private static final Long ORG_ID = 100001L;
    private static final Long OPERATOR_USER_ID = 100001L;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AnalysisProperties analysisProperties;

    @MockBean
    private ObjectStorageService objectStorageService;

    private final AtomicLong idSequence = new AtomicLong(System.currentTimeMillis() * 1000L);
    private final List<TestFixture> fixtures = new ArrayList<>();

    @BeforeEach
    void setUpStorageMock() throws Exception {
        org.mockito.BDDMockito.given(objectStorageService.store(
                        any(String.class),
                        any(String.class),
                        any(InputStream.class),
                        anyLong(),
                        any(String.class)))
                .willAnswer(invocation -> {
                    String originalFileName = invocation.getArgument(0, String.class);
                    String contentType = invocation.getArgument(1, String.class);
                    long fileSize = invocation.getArgument(3, Long.class);
                    String md5 = invocation.getArgument(4, String.class);
                    String safeName = originalFileName == null ? "report.pdf" : originalFileName;
                    return new StoredObject(
                            "e2e-bucket",
                            "e2e/" + UUID.randomUUID() + "/" + safeName,
                            safeName,
                            contentType,
                            fileSize,
                            md5,
                            "MINIO");
                });
        org.mockito.BDDMockito.willDoNothing().given(objectStorageService).delete(any(String.class), any(String.class));
    }

    @AfterEach
    void tearDownFixtureAndSecurity() {
        for (TestFixture fixture : fixtures) {
            cleanupFixture(fixture);
        }
        fixtures.clear();
        SecurityContextHolder.clearContext();
    }

    protected TestFixture createFixture(String initialCaseStatus) {
        long seed = idSequence.addAndGet(100L);
        String tag = "e2e_" + seed;
        long patientId = seed + 1;
        long visitId = seed + 2;
        long caseId = seed + 3;
        long imageAttachmentId = seed + 4;
        long imageId = seed + 5;
        long qualityCheckId = seed + 6;
        long visualAssetAttachmentId = seed + 7;
        long initStatusLogId = seed + 8;
        long templateId = seed + 9;
        String patientNo = "PAT" + seed;
        String visitNo = "VIS" + seed;
        String caseNo = "CASE" + seed;
        String templateCode = "TPL_DOCTOR_" + seed;

        jdbcTemplate.update("""
                INSERT INTO pat_patient
                (id, patient_no, patient_name_enc, patient_name_hash, patient_name_masked,
                 gender_code, age, source_code, privacy_level_code, org_id, status, deleted_flag, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, 'UNKNOWN', 12, 'MANUAL', 'L4', ?, 'ACTIVE', 0, ?, ?)
                """,
                patientId, patientNo, "enc_" + tag, "hash_" + tag, "P*" + seed, ORG_ID, OPERATOR_USER_ID, OPERATOR_USER_ID);

        jdbcTemplate.update("""
                INSERT INTO med_visit
                (id, visit_no, patient_id, doctor_user_id, visit_type_code, visit_date,
                 triage_level_code, source_channel_code, org_id, status, deleted_flag, created_by, updated_by)
                VALUES (?, ?, ?, ?, 'OUTPATIENT', NOW(), 'NORMAL', 'MANUAL', ?, 'ACTIVE', 0, ?, ?)
                """,
                visitId, visitNo, patientId, OPERATOR_USER_ID, ORG_ID, OPERATOR_USER_ID, OPERATOR_USER_ID);

        jdbcTemplate.update("""
                INSERT INTO med_case
                (id, case_no, visit_id, patient_id, case_title, case_type_code, case_status_code, priority_code,
                 report_ready_flag, followup_required_flag, org_id, version_no, status, deleted_flag, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, 'CARIES_SCREENING', ?, 'NORMAL', '0', '0', ?, 1, 'ACTIVE', 0, ?, ?)
                """,
                caseId, caseNo, visitId, patientId, "E2E " + tag, initialCaseStatus, ORG_ID, OPERATOR_USER_ID, OPERATOR_USER_ID);

        jdbcTemplate.update("""
                INSERT INTO med_case_status_log
                (id, case_id, from_status_code, to_status_code, changed_by, change_reason_code, change_reason, changed_at, org_id)
                VALUES (?, ?, NULL, ?, ?, 'E2E_INIT', 'fixture initialized', NOW(), ?)
                """,
                initStatusLogId, caseId, initialCaseStatus, OPERATOR_USER_ID, ORG_ID);

        jdbcTemplate.update("""
                INSERT INTO med_attachment
                (id, biz_module_code, biz_id, file_category_code, file_name, original_name, bucket_name, object_key,
                 content_type, file_ext, file_size_bytes, md5, storage_provider_code, visibility_code, upload_user_id,
                 org_id, status, deleted_flag, remark, created_by, updated_by)
                VALUES (?, 'CASE', ?, 'IMAGE', ?, ?, 'e2e-bucket', ?, 'image/jpeg', 'jpg', 1024, ?, 'MINIO', 'PRIVATE',
                        ?, ?, 'ACTIVE', 0, 'e2e source image', ?, ?)
                """,
                imageAttachmentId,
                caseId,
                "e2e-image-" + tag + ".jpg",
                "e2e-image-" + tag + ".jpg",
                "e2e/case/" + tag + "/image.jpg",
                "md5-image-" + tag,
                OPERATOR_USER_ID,
                ORG_ID,
                OPERATOR_USER_ID,
                OPERATOR_USER_ID);

        jdbcTemplate.update("""
                INSERT INTO med_attachment
                (id, biz_module_code, biz_id, file_category_code, file_name, original_name, bucket_name, object_key,
                 content_type, file_ext, file_size_bytes, md5, storage_provider_code, visibility_code, upload_user_id,
                 org_id, status, deleted_flag, remark, created_by, updated_by)
                VALUES (?, 'ANALYSIS', ?, 'IMAGE', ?, ?, 'e2e-bucket', ?, 'image/png', 'png', 2048, ?, 'MINIO', 'PRIVATE',
                        ?, ?, 'ACTIVE', 0, 'e2e visual asset', ?, ?)
                """,
                visualAssetAttachmentId,
                caseId,
                "e2e-asset-" + tag + ".png",
                "e2e-asset-" + tag + ".png",
                "e2e/analysis/" + tag + "/heatmap.png",
                "md5-asset-" + tag,
                OPERATOR_USER_ID,
                ORG_ID,
                OPERATOR_USER_ID,
                OPERATOR_USER_ID);

        jdbcTemplate.update("""
                INSERT INTO med_image_file
                (id, case_id, visit_id, patient_id, attachment_id, image_type_code, image_source_code, shooting_time,
                 quality_status_code, is_primary, org_id, status, deleted_flag, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, 'PANORAMIC', 'UPLOAD', NOW(), 'PASS', '1', ?, 'ACTIVE', 0, ?, ?)
                """,
                imageId, caseId, visitId, patientId, imageAttachmentId, ORG_ID, OPERATOR_USER_ID, OPERATOR_USER_ID);

        jdbcTemplate.update("""
                INSERT INTO med_image_quality_check
                (id, image_id, case_id, patient_id, check_type_code, check_result_code, quality_score, current_flag,
                 checked_by, org_id, status, deleted_flag, created_by, updated_by)
                VALUES (?, ?, ?, ?, 'AUTO', 'PASS', 96, '1', ?, ?, 'ACTIVE', 0, ?, ?)
                """,
                qualityCheckId, imageId, caseId, patientId, OPERATOR_USER_ID, ORG_ID, OPERATOR_USER_ID, OPERATOR_USER_ID);

        jdbcTemplate.update("""
                INSERT INTO rpt_template
                (id, template_code, template_name, report_type_code, template_content, version_no, org_id,
                 status, deleted_flag, remark, created_by, updated_by)
                VALUES (?, ?, ?, 'DOCTOR', ?, 1, ?, 'ACTIVE', 0, 'e2e doctor template', ?, ?)
                """,
                templateId,
                templateCode,
                "Doctor Template " + tag,
                """
                        Report No: {{reportNo}}
                        Case No: {{caseNo}}
                        Highest Severity: {{highestSeverity}}
                        Risk Level: {{riskLevelCode}}
                        Doctor Conclusion: {{doctorConclusion}}
                        """,
                ORG_ID,
                OPERATOR_USER_ID,
                OPERATOR_USER_ID);

        TestFixture fixture = new TestFixture(
                tag,
                ORG_ID,
                OPERATOR_USER_ID,
                patientId,
                visitId,
                caseId,
                imageAttachmentId,
                visualAssetAttachmentId,
                templateId);
        fixtures.add(fixture);
        return fixture;
    }

    protected void authenticateAsSysAdmin(Long orgId) {
        AuthenticatedUser user = new AuthenticatedUser(
                OPERATOR_USER_ID,
                orgId,
                "admin",
                "N/A",
                "E2E Admin",
                true,
                List.of("SYS_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "N/A", user.getAuthorities()));
    }

    protected AnalysisTaskRef createAnalysisTask(TestFixture fixture, String remark) throws Exception {
        authenticateAsSysAdmin(fixture.orgId());
        JsonNode body = postJson(
                "/api/v1/analysis/tasks",
                Map.of(
                        "caseId", fixture.caseId(),
                        "patientId", fixture.patientId(),
                        "forceRetryFlag", false,
                        "taskTypeCode", "INFERENCE",
                        "remark", remark),
                Map.of(),
                200);
        assertThat(body.path("code").asText()).isEqualTo("00000");
        JsonNode data = body.path("data");
        return new AnalysisTaskRef(data.path("taskId").asLong(), data.path("taskNo").asText());
    }

    protected JsonNode callbackSuccess(AnalysisTaskRef task, TestFixture fixture) throws Exception {
        String callbackBody = buildSuccessCallbackBody(task.taskNo(), fixture.visualAssetAttachmentId());
        String timestamp = String.valueOf(java.time.Instant.now().getEpochSecond());
        String signature = signCallback(callbackBody, timestamp);
        JsonNode body = postRawJson(
                "/api/v1/internal/ai/callbacks/analysis-result",
                callbackBody,
                Map.of(
                        "X-AI-Timestamp", timestamp,
                        "X-AI-Signature", signature),
                200);
        assertThat(body.path("code").asText()).isEqualTo("00000");
        return body;
    }

    protected long generateDoctorReport(TestFixture fixture, String doctorConclusion) throws Exception {
        authenticateAsSysAdmin(fixture.orgId());
        JsonNode body = postJson(
                "/api/v1/cases/" + fixture.caseId() + "/reports",
                Map.of(
                        "reportTypeCode", "DOCTOR",
                        "doctorConclusion", doctorConclusion,
                        "remark", "e2e report"),
                Map.of(),
                200);
        assertThat(body.path("code").asText()).isEqualTo("00000");
        return body.path("data").path("reportId").asLong();
    }

    protected JsonNode exportReport(long reportId) throws Exception {
        authenticateAsSysAdmin(ORG_ID);
        JsonNode body = postJson(
                "/api/v1/reports/" + reportId + "/export",
                Map.of(
                        "exportTypeCode", "PDF",
                        "exportChannelCode", "DOWNLOAD"),
                Map.of(),
                200);
        assertThat(body.path("code").asText()).isEqualTo("00000");
        return body;
    }

    protected JsonNode exportReportWithoutBody(long reportId) throws Exception {
        authenticateAsSysAdmin(ORG_ID);
        JsonNode body = postRawJson(
                "/api/v1/reports/" + reportId + "/export",
                "",
                Map.of(),
                200);
        assertThat(body.path("code").asText()).isEqualTo("00000");
        return body;
    }

    protected JsonNode attemptGenerateReportBeforeReady(TestFixture fixture) throws Exception {
        authenticateAsSysAdmin(fixture.orgId());
        return postJson(
                "/api/v1/cases/" + fixture.caseId() + "/reports",
                Map.of(
                        "reportTypeCode", "DOCTOR",
                        "doctorConclusion", "not ready",
                        "remark", "should fail"),
                Map.of(),
                400);
    }

    protected JsonNode updateFollowupTaskStatus(TestFixture fixture, long taskId, String targetStatusCode, String remark) throws Exception {
        authenticateAsSysAdmin(fixture.orgId());
        JsonNode body = postJson(
                "/api/v1/followup/tasks/" + taskId + "/status",
                Map.of(
                        "targetStatusCode", targetStatusCode,
                        "remark", remark),
                Map.of(),
                200);
        assertThat(body.path("code").asText()).isEqualTo("00000");
        return body;
    }

    protected JsonNode addFollowupRecord(TestFixture fixture,
                                         long taskId,
                                         boolean followNext,
                                         Integer nextIntervalDays,
                                         String outcomeSummary) throws Exception {
        authenticateAsSysAdmin(fixture.orgId());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("taskId", taskId);
        request.put("followupMethodCode", "PHONE");
        request.put("contactResultCode", "REACHED");
        request.put("followNext", followNext);
        request.put("nextIntervalDays", nextIntervalDays);
        request.put("outcomeSummary", outcomeSummary);
        request.put("doctorNotes", "boot e2e record");
        request.put("remark", "boot e2e record");
        JsonNode body = postJson(
                "/api/v1/followup/records",
                request,
                Map.of(),
                200);
        assertThat(body.path("code").asText()).isEqualTo("00000");
        return body;
    }

    protected long count(String sql, Object... args) {
        Number value = jdbcTemplate.queryForObject(sql, Number.class, args);
        return value == null ? 0L : value.longValue();
    }

    protected String queryString(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, String.class, args);
    }

    protected Map<String, Object> queryRow(String sql, Object... args) {
        return jdbcTemplate.queryForMap(sql, args);
    }

    protected boolean callbackIsIdempotent(JsonNode callbackResponse) {
        JsonNode data = callbackResponse.path("data");
        if (data.has("idempotent")) {
            return data.path("idempotent").asBoolean();
        }
        return data.path("duplicate").asBoolean();
    }

    private JsonNode postJson(String url, Object requestBody, Map<String, String> headers, int expectedStatus) throws Exception {
        String json = objectMapper.writeValueAsString(requestBody);
        return postRawJson(url, json, headers, expectedStatus);
    }

    private JsonNode postRawJson(String url, String rawBody, Map<String, String> headers, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder builder = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post(url)
                .contentType(APPLICATION_JSON);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }
        if (rawBody != null) {
            builder.content(rawBody);
        }
        MvcResult result = mockMvc.perform(builder).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String buildSuccessCallbackBody(String taskNo, Long visualAssetAttachmentId) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("taskNo", taskNo);
        root.put("taskStatusCode", "SUCCESS");
        root.put("startedAt", LocalDateTime.now().minusSeconds(20).toString());
        root.put("completedAt", LocalDateTime.now().toString());
        root.put("modelVersion", "caries-v1");

        ObjectNode summary = root.putObject("summary");
        summary.put("overallHighestSeverity", "C2");
        summary.put("uncertaintyScore", 0.2700d);
        summary.put("reviewSuggestedFlag", "1");
        summary.put("teethCount", 8);

        ObjectNode rawResult = root.putObject("rawResultJson");
        rawResult.put("overallHighestSeverity", "C2");
        rawResult.put("uncertaintyScore", 0.2700d);
        rawResult.put("reviewSuggestedFlag", "1");

        ArrayNode assets = root.putArray("visualAssets");
        ObjectNode heatmap = assets.addObject();
        heatmap.put("assetTypeCode", "HEATMAP");
        heatmap.put("attachmentId", visualAssetAttachmentId);

        ObjectNode risk = root.putObject("riskAssessment");
        risk.put("overallRiskLevelCode", "HIGH");
        ObjectNode riskJson = risk.putObject("assessmentReportJson");
        riskJson.put("factor", "SUGAR");
        risk.put("recommendedCycleDays", 30);
        return root.toString();
    }

    private String signCallback(String body, String timestamp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(analysisProperties.getCallbackSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign callback", exception);
        }
    }

    private void cleanupFixture(TestFixture fixture) {
        try {
            jdbcTemplate.update("DELETE FROM rpt_export_log WHERE report_id IN (SELECT id FROM rpt_record WHERE case_id = ?)", fixture.caseId());
            jdbcTemplate.update("DELETE FROM med_attachment WHERE biz_module_code = 'REPORT' AND biz_id IN (SELECT id FROM rpt_record WHERE case_id = ?)", fixture.caseId());
            jdbcTemplate.update("DELETE FROM rpt_record WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM msg_notify_record WHERE biz_module_code = 'FOLLOWUP' AND biz_id IN (SELECT id FROM fup_task WHERE case_id = ?)", fixture.caseId());
            jdbcTemplate.update("DELETE FROM fup_record WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM fup_task WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM fup_plan WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM ana_visual_asset WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM ana_result_summary WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM med_risk_assessment_record WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM ana_task_record WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM med_image_quality_check WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM med_image_file WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM med_attachment WHERE id IN (?, ?)", fixture.imageAttachmentId(), fixture.visualAssetAttachmentId());
            jdbcTemplate.update("DELETE FROM med_case_status_log WHERE case_id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM rpt_template WHERE id = ?", fixture.templateId());
            jdbcTemplate.update("DELETE FROM med_case WHERE id = ?", fixture.caseId());
            jdbcTemplate.update("DELETE FROM med_visit WHERE id = ?", fixture.visitId());
            jdbcTemplate.update("DELETE FROM pat_patient WHERE id = ?", fixture.patientId());
        } catch (Exception ignored) {
            // Keep cleanup best-effort to avoid cascading failures in following tests.
        }
    }

    protected record TestFixture(
            String tag,
            Long orgId,
            Long operatorUserId,
            Long patientId,
            Long visitId,
            Long caseId,
            Long imageAttachmentId,
            Long visualAssetAttachmentId,
            Long templateId) {
    }

    protected record AnalysisTaskRef(Long taskId, String taskNo) {
    }
}
