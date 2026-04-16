package com.cariesguard.boot.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class MainlineWorkflowE2ETest extends AnalysisReportE2EBaseTest {

    private static final Long ORG_ID = 100001L;

    private final List<Long> uploadedAttachmentIds = new ArrayList<>();

    private Long patientId;
    private Long visitId;
    private Long caseId;
    private Long reportId;
    private Long templateId;

    @AfterEach
    void cleanupMainlineWorkflowData() {
        try {
            if (reportId != null) {
                jdbcTemplate.update("DELETE FROM rpt_export_log WHERE report_id = ?", reportId);
            }
            if (caseId != null) {
                jdbcTemplate.update("DELETE FROM med_attachment WHERE biz_module_code = 'REPORT' AND biz_id IN (SELECT id FROM rpt_record WHERE case_id = ?)", caseId);
                jdbcTemplate.update("DELETE FROM rpt_record WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM msg_notify_record WHERE biz_module_code = 'FOLLOWUP' AND biz_id IN (SELECT id FROM fup_task WHERE case_id = ?)", caseId);
                jdbcTemplate.update("DELETE FROM fup_record WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM fup_task WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM fup_plan WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM ana_correction_feedback WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM ana_visual_asset WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM ana_result_summary WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM med_risk_assessment_record WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM ana_task_record WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM med_image_quality_check WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM med_image_file WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM med_case_status_log WHERE case_id = ?", caseId);
                jdbcTemplate.update("DELETE FROM med_case WHERE id = ?", caseId);
            }
            if (visitId != null) {
                jdbcTemplate.update("DELETE FROM med_visit WHERE id = ?", visitId);
            }
            if (patientId != null) {
                jdbcTemplate.update("DELETE FROM pat_guardian WHERE patient_id = ?", patientId);
                jdbcTemplate.update("DELETE FROM pat_profile WHERE patient_id = ?", patientId);
                jdbcTemplate.update("DELETE FROM pat_patient WHERE id = ?", patientId);
            }
            if (templateId != null) {
                jdbcTemplate.update("DELETE FROM rpt_template WHERE id = ?", templateId);
            }
            if (!uploadedAttachmentIds.isEmpty()) {
                jdbcTemplate.update("DELETE FROM med_attachment WHERE id IN (" + placeholders(uploadedAttachmentIds.size()) + ")",
                        uploadedAttachmentIds.toArray());
            }
        } catch (Exception ignored) {
            // Keep cleanup best-effort.
        }
    }

    @Test
    void shouldRunMainlineWorkflowThroughRealApisAndUpdateAuditAndDashboard() throws Exception {
        long loginLogBefore = count("SELECT COUNT(1) FROM sys_login_log WHERE username = 'admin'");
        JsonNode overviewBefore = getJson("/api/v1/dashboard/overview", ORG_ID).path("data");
        JsonNode followupSummaryBefore = getJson("/api/v1/dashboard/followup-task-summary", ORG_ID).path("data");

        JsonNode login = postJson(
                "/api/v1/auth/login",
                Map.of("username", "admin", "password", "123456"),
                Map.of(),
                200);
        assertThat(login.path("code").asText()).isEqualTo("00000");
        assertThat(login.path("data").path("token").asText()).isNotBlank();
        assertThat(login.path("data").path("expireIn").asLong()).isPositive();
        assertThat(login.path("data").path("user").path("username").asText()).isEqualTo("admin");
        assertThat(count("SELECT COUNT(1) FROM sys_login_log WHERE username = 'admin'")).isEqualTo(loginLogBefore + 1);
        Map<String, Object> latestLoginLog = queryRow("""
                SELECT username, login_status_code, user_id, org_id
                FROM sys_login_log
                WHERE username = 'admin'
                ORDER BY login_time DESC
                LIMIT 1
                """);
        assertThat(latestLoginLog.get("login_status_code")).isEqualTo("SUCCESS");
        assertThat(((Number) latestLoginLog.get("user_id")).longValue()).isEqualTo(100001L);
        assertThat(((Number) latestLoginLog.get("org_id")).longValue()).isEqualTo(ORG_ID);

        JsonNode currentUser = getJson("/api/v1/auth/me", ORG_ID).path("data");
        assertThat(currentUser.path("userId").asLong()).isEqualTo(100001L);
        assertThat(currentUser.path("orgId").asLong()).isEqualTo(ORG_ID);
        assertThat(currentUser.path("username").asText()).isEqualTo("admin");

        templateId = createDoctorTemplate();
        patientId = createPatient();
        assertPatientProtection();
        visitId = createVisit(patientId);
        caseId = createCase(patientId, visitId);
        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", caseId)).isEqualTo("CREATED");
        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND to_status_code = 'CREATED'
                  AND change_reason_code = 'CASE_CREATED'
                """, caseId)).isEqualTo(1);

        long sourceAttachmentId = uploadFile("mainline-image.jpg", "image/jpeg", uniqueBytes("source-image"));
        long imageId = createCaseImage(caseId, visitId, patientId, sourceAttachmentId);
        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", caseId)).isEqualTo("QC_PENDING");
        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'CREATED'
                  AND to_status_code = 'QC_PENDING'
                  AND change_reason_code = 'IMAGE_UPLOADED'
                """, caseId)).isEqualTo(1);

        saveQualityCheck(imageId);
        JsonNode qualityCheck = getJson("/api/v1/images/" + imageId + "/quality-checks/current", ORG_ID).path("data");
        assertThat(qualityCheck.path("checkResultCode").asText()).isEqualTo("PASS");
        assertThat(qualityCheck.path("qualityScore").asInt()).isEqualTo(97);

        long visualAssetAttachmentId = uploadFile("mainline-asset.png", "image/png", uniqueBytes("visual-asset"));
        AnalysisTaskRef analysisTask = createAnalysisTaskFromCase(caseId, patientId);
        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", caseId)).isEqualTo("ANALYZING");
        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'QC_PENDING'
                  AND to_status_code = 'ANALYZING'
                  AND change_reason_code = 'QC_PASSED'
                """, caseId)).isEqualTo(1);

        JsonNode callback = callbackSuccess(analysisTask, visualAssetAttachmentId);
        assertThat(callback.path("data").path("taskStatusCode").asText()).isEqualTo("SUCCESS");
        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", caseId)).isEqualTo("REVIEW_PENDING");
        assertThat(count("SELECT COUNT(1) FROM ana_result_summary WHERE case_id = ?", caseId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(1) FROM ana_visual_asset WHERE case_id = ?", caseId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(1) FROM med_risk_assessment_record WHERE case_id = ?", caseId)).isEqualTo(1);

        long correctionId = submitCorrection(caseId, imageId);
        assertThat(correctionId).isPositive();
        assertThat(count("SELECT COUNT(1) FROM ana_correction_feedback WHERE case_id = ?", caseId)).isEqualTo(1);

        reportId = generateDoctorReport(caseId);
        assertThat(reportId).isPositive();
        assertThat(queryString("SELECT case_status_code FROM med_case WHERE id = ?", caseId)).isEqualTo("FOLLOWUP_REQUIRED");
        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'REVIEW_PENDING'
                  AND to_status_code = 'REPORT_READY'
                  AND change_reason_code = 'DOCTOR_CONFIRMED'
                """, caseId)).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(1)
                FROM med_case_status_log
                WHERE case_id = ?
                  AND from_status_code = 'REPORT_READY'
                  AND to_status_code = 'FOLLOWUP_REQUIRED'
                  AND change_reason_code = 'FOLLOWUP_TRIGGERED'
                """, caseId)).isEqualTo(1);

        JsonNode export = exportReport(reportId);
        assertThat(export.path("data").path("exported").asBoolean()).isTrue();
        assertThat(count("SELECT COUNT(1) FROM rpt_export_log WHERE report_id = ?", reportId)).isEqualTo(1);

        JsonNode followupTasks = getJson("/api/v1/cases/" + caseId + "/followup/tasks", ORG_ID).path("data");
        assertThat(followupTasks).hasSize(1);
        long followupTaskId = followupTasks.get(0).path("taskId").asLong();
        long followupPlanId = followupTasks.get(0).path("planId").asLong();
        assertThat(followupTasks.get(0).path("taskStatusCode").asText()).isEqualTo("TODO");
        assertThat(count("""
                SELECT COUNT(1)
                FROM msg_notify_record
                WHERE biz_module_code = 'FOLLOWUP' AND biz_id = ?
                """, followupTaskId)).isGreaterThanOrEqualTo(1);

        addFollowupRecord(followupTaskId);
        assertThat(queryString("SELECT plan_status_code FROM fup_plan WHERE id = ?", followupPlanId)).isEqualTo("DONE");
        assertThat(queryString("SELECT task_status_code FROM fup_task WHERE id = ?", followupTaskId)).isEqualTo("DONE");
        assertThat(count("SELECT COUNT(1) FROM fup_record WHERE task_id = ?", followupTaskId)).isEqualTo(1);

        JsonNode overviewAfter = getJson("/api/v1/dashboard/overview", ORG_ID).path("data");
        assertThat(overviewAfter.path("patientCount").asLong()).isEqualTo(overviewBefore.path("patientCount").asLong() + 1);
        assertThat(overviewAfter.path("caseCount").asLong()).isEqualTo(overviewBefore.path("caseCount").asLong() + 1);
        assertThat(overviewAfter.path("analyzedCaseCount").asLong()).isEqualTo(overviewBefore.path("analyzedCaseCount").asLong() + 1);
        assertThat(overviewAfter.path("generatedReportCount").asLong()).isEqualTo(overviewBefore.path("generatedReportCount").asLong() + 1);
        assertThat(overviewAfter.path("followupRequiredCaseCount").asLong()).isEqualTo(overviewBefore.path("followupRequiredCaseCount").asLong() + 1);

        JsonNode followupSummaryAfter = getJson("/api/v1/dashboard/followup-task-summary", ORG_ID).path("data");
        assertThat(followupSummaryAfter.path("doneCount").asLong()).isEqualTo(followupSummaryBefore.path("doneCount").asLong() + 1);
    }

    private Long createDoctorTemplate() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        JsonNode body = postJsonAsAdmin(
                "/api/v1/report-templates",
                Map.of(
                        "templateCode", "MAINLINE_DOCTOR_" + suffix,
                        "templateName", "Mainline Doctor Template " + suffix,
                        "reportTypeCode", "DOCTOR",
                        "templateContent", """
                                Report No: {{reportNo}}
                                Case No: {{caseNo}}
                                Highest Severity: {{highestSeverity}}
                                Risk Level: {{riskLevelCode}}
                                Doctor Conclusion: {{doctorConclusion}}
                                """
                ));
        return body.path("data").path("templateId").asLong();
    }

    private Long createPatient() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        JsonNode body = postJsonAsAdmin(
                "/api/v1/patients",
                Map.of(
                        "patientName", "测试患者" + suffix,
                        "genderCode", "FEMALE",
                        "birthDate", "2016-05-20",
                        "phone", "1390000" + suffix.substring(Math.max(0, suffix.length() - 4)),
                        "idCardNo", "33010120160520" + suffix.substring(Math.max(0, suffix.length() - 4)),
                        "sourceCode", "OUTPATIENT",
                        "privacyLevelCode", "L4",
                        "guardian", Map.of(
                                "guardianName", "家长" + suffix,
                                "relationCode", "PARENT",
                                "phone", "1380000" + suffix.substring(Math.max(0, suffix.length() - 4)),
                                "certificateTypeCode", "ID_CARD",
                                "certificateNo", "33010119881212" + suffix.substring(Math.max(0, suffix.length() - 4)),
                                "primaryFlag", "1"
                        )));
        return body.path("data").path("patientId").asLong();
    }

    private void assertPatientProtection() throws Exception {
        JsonNode patientDetail = getJson("/api/v1/patients/" + patientId, ORG_ID).path("data");
        assertThat(patientDetail.path("patientId").asLong()).isEqualTo(patientId);
        assertThat(patientDetail.path("patientNameMasked").asText()).contains("*");
        assertThat(patientDetail.path("guardianList")).hasSize(1);
        assertThat(patientDetail.path("guardianList").get(0).path("guardianNameMasked").asText()).contains("*");

        Map<String, Object> patientRow = queryRow("""
                SELECT patient_name_enc, patient_name_hash, patient_name_masked, phone_enc, phone_hash, phone_masked
                FROM pat_patient
                WHERE id = ?
                """, patientId);
        assertThat(patientRow.get("patient_name_enc")).isNotNull();
        assertThat(patientRow.get("patient_name_hash")).isNotNull();
        assertThat(patientRow.get("patient_name_masked").toString()).contains("*");
        assertThat(patientRow.get("phone_enc")).isNotNull();
        assertThat(patientRow.get("phone_hash")).isNotNull();
        assertThat(patientRow.get("phone_masked").toString()).contains("*");
    }

    private Long createVisit(Long patientId) throws Exception {
        JsonNode body = postJsonAsAdmin(
                "/api/v1/visits",
                Map.of(
                        "patientId", patientId,
                        "doctorUserId", 100001L,
                        "visitTypeCode", "OUTPATIENT",
                        "visitDate", LocalDateTime.now().toString(),
                        "complaint", "tooth pain",
                        "triageLevelCode", "NORMAL",
                        "sourceChannelCode", "MANUAL"
                ));
        return body.path("data").path("visitId").asLong();
    }

    private Long createCase(Long patientId, Long visitId) throws Exception {
        JsonNode body = postJsonAsAdmin(
                "/api/v1/cases",
                Map.of(
                        "visitId", visitId,
                        "patientId", patientId,
                        "caseTypeCode", "CARIES_SCREENING",
                        "caseTitle", "Mainline Case",
                        "chiefComplaint", "screening",
                        "priorityCode", "NORMAL"
                ));
        assertThat(body.path("data").path("caseStatusCode").asText()).isEqualTo("CREATED");
        return body.path("data").path("caseId").asLong();
    }

    private Long uploadFile(String originalName, String contentType, byte[] bytes) throws Exception {
        authenticateAsSysAdmin(ORG_ID);
        MockMultipartFile file = new MockMultipartFile("file", originalName, contentType, bytes);
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/files/upload")
                .file(file)
                .param("caseId", String.valueOf(caseId))
                .param("imageTypeCode", "PANORAMIC")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("code").asText()).isEqualTo("00000");
        long attachmentId = body.path("data").path("attachmentId").asLong();
        uploadedAttachmentIds.add(attachmentId);
        return attachmentId;
    }

    private Long createCaseImage(Long caseId, Long visitId, Long patientId, Long attachmentId) throws Exception {
        JsonNode body = postJsonAsAdmin(
                "/api/v1/cases/" + caseId + "/images",
                Map.of(
                        "attachmentId", attachmentId,
                        "visitId", visitId,
                        "patientId", patientId,
                        "imageTypeCode", "PANORAMIC",
                        "imageSourceCode", "UPLOAD",
                        "shootingTime", LocalDateTime.now().toString(),
                        "primaryFlag", "1"
                ));
        assertThat(body.path("data").path("qualityStatusCode").asText()).isEqualTo("PENDING");
        return body.path("data").path("imageId").asLong();
    }

    private void saveQualityCheck(Long imageId) throws Exception {
        JsonNode body = postJsonAsAdmin(
                "/api/v1/images/" + imageId + "/quality-checks",
                Map.of(
                        "checkTypeCode", "AUTO",
                        "checkResultCode", "PASS",
                        "qualityScore", 97,
                        "issueCodes", List.of("NONE"),
                        "suggestionText", "good image"
                ));
        assertThat(body.path("data").path("checkResultCode").asText()).isEqualTo("PASS");
    }

    private AnalysisTaskRef createAnalysisTaskFromCase(Long caseId, Long patientId) throws Exception {
        JsonNode body = postJsonAsAdmin(
                "/api/v1/cases/" + caseId + "/analysis",
                Map.of(
                        "caseId", caseId,
                        "patientId", patientId,
                        "forceRetryFlag", false,
                        "taskTypeCode", "INFERENCE",
                        "remark", "mainline workflow"
                ));
        return new AnalysisTaskRef(
                body.path("data").path("taskId").asLong(),
                body.path("data").path("taskNo").asText());
    }

    private Long submitCorrection(Long caseId, Long sourceImageId) throws Exception {
        ObjectNode original = objectMapper.createObjectNode();
        original.put("severity", "C2");
        ObjectNode corrected = objectMapper.createObjectNode();
        corrected.put("severity", "C1");
        corrected.put("doctorConclusion", "manual downgrade");
        JsonNode body = postJsonAsAdmin(
                "/api/v1/cases/" + caseId + "/corrections",
                Map.of(
                        "caseId", caseId,
                        "sourceImageId", sourceImageId,
                        "feedbackTypeCode", "RE_GRADE",
                        "originalInferenceJson", original,
                        "correctedTruthJson", corrected
                ));
        return body.path("data").path("feedbackId").asLong();
    }

    private Long generateDoctorReport(Long caseId) throws Exception {
        JsonNode body = postJsonAsAdmin(
                "/api/v1/cases/" + caseId + "/reports",
                Map.of(
                        "reportTypeCode", "DOCTOR",
                        "doctorConclusion", "confirmed by doctor",
                        "remark", "mainline workflow report"
                ));
        return body.path("data").path("reportId").asLong();
    }

    private JsonNode exportReport(Long reportId) throws Exception {
        return postJsonAsAdmin(
                "/api/v1/reports/" + reportId + "/export",
                Map.of(
                        "exportTypeCode", "PDF",
                        "exportChannelCode", "DOWNLOAD"
                ));
    }

    private void addFollowupRecord(Long taskId) throws Exception {
        JsonNode body = postJsonAsAdmin(
                "/api/v1/followup/records",
                Map.of(
                        "taskId", taskId,
                        "followupMethodCode", "PHONE",
                        "contactResultCode", "REACHED",
                        "followNext", false,
                        "outcomeSummary", "followup completed",
                        "doctorNotes", "no further action"
                ));
        assertThat(body.path("data").path("taskId").asLong()).isEqualTo(taskId);
    }

    private JsonNode postJsonAsAdmin(String url, Object requestBody) throws Exception {
        authenticateAsSysAdmin(ORG_ID);
        JsonNode body = postJson(url, requestBody, Map.of(), 200);
        assertThat(body.path("code").asText()).isEqualTo("00000");
        return body;
    }

    private byte[] uniqueBytes(String prefix) {
        return (prefix + "-" + System.nanoTime()).getBytes(StandardCharsets.UTF_8);
    }

    private String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }
}
