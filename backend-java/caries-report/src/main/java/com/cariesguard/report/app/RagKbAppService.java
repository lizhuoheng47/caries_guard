package com.cariesguard.report.app;

import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.infrastructure.client.HttpRagAdminClient;
import com.cariesguard.report.interfaces.command.RagKbImportTextCommand;
import com.cariesguard.report.interfaces.command.RagKbRebuildCommand;
import com.cariesguard.report.interfaces.command.RagKbUpdateCommand;
import com.cariesguard.report.interfaces.command.RagVersionActionCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RagKbAppService {

    private final HttpRagAdminClient ragAdminClient;

    public RagKbAppService(HttpRagAdminClient ragAdminClient) {
        this.ragAdminClient = ragAdminClient;
    }

    public Object overview(String kbCode) {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("kb_code", kbCode);
        query.put("org_id", user.getOrgId());
        return ragAdminClient.get("/knowledge/overview", query, TraceIdUtils.currentTraceId());
    }

    public Object documents(String kbCode, String keyword) {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("kb_code", kbCode);
        query.put("keyword", keyword);
        query.put("org_id", user.getOrgId());
        return ragAdminClient.get("/knowledge/documents", query, TraceIdUtils.currentTraceId());
    }

    public Object documentDetail(Long docId) {
        return ragAdminClient.get("/knowledge/documents/" + docId, Map.of(), TraceIdUtils.currentTraceId());
    }

    public Object importText(RagKbImportTextCommand command) {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceId", TraceIdUtils.currentTraceId());
        payload.put("kbCode", command.kbCode());
        payload.put("kbName", command.kbName());
        payload.put("kbTypeCode", command.kbTypeCode());
        payload.put("docNo", command.docNo());
        payload.put("docTitle", command.docTitle());
        payload.put("docSourceCode", command.docSourceCode());
        payload.put("sourceUri", command.sourceUri());
        payload.put("docVersion", command.docVersion());
        payload.put("contentText", command.contentText());
        payload.put("reviewStatusCode", command.reviewStatusCode());
        payload.put("orgId", user.getOrgId());
        return ragAdminClient.post("/knowledge/documents", payload, TraceIdUtils.currentTraceId());
    }

    public Object upload(
            MultipartFile file,
            String kbCode,
            String kbName,
            String kbTypeCode,
            String docTitle,
            String docSourceCode,
            String sourceUri,
            String docNo,
            String docVersion,
            String changeSummary) {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("kb_code", kbCode);
        form.put("kb_name", kbName);
        form.put("kb_type_code", kbTypeCode);
        form.put("doc_title", docTitle);
        form.put("doc_source_code", docSourceCode);
        form.put("source_uri", sourceUri);
        form.put("doc_no", docNo);
        form.put("doc_version", docVersion);
        form.put("change_summary", changeSummary);
        form.put("org_id", user.getOrgId());
        form.put("operator_id", user.getUserId());
        form.put("trace_id", TraceIdUtils.currentTraceId());
        return ragAdminClient.multipart("/knowledge/documents/upload", file, form, TraceIdUtils.currentTraceId());
    }

    public Object update(Long docId, RagKbUpdateCommand command) {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceId", TraceIdUtils.currentTraceId());
        payload.put("docTitle", command.docTitle());
        payload.put("docSourceCode", command.docSourceCode());
        payload.put("sourceUri", command.sourceUri());
        payload.put("contentText", command.contentText());
        payload.put("changeSummary", command.changeSummary());
        payload.put("operatorId", user.getUserId());
        return ragAdminClient.put("/knowledge/documents/" + docId, payload, TraceIdUtils.currentTraceId());
    }

    public Object submitReview(Long docId, RagVersionActionCommand command) {
        return versionAction("/knowledge/documents/" + docId + "/submit-review", command);
    }

    public Object approve(Long docId, RagVersionActionCommand command) {
        return versionAction("/knowledge/documents/" + docId + "/approve", command);
    }

    public Object reject(Long docId, RagVersionActionCommand command) {
        return versionAction("/knowledge/documents/" + docId + "/reject", command);
    }

    public Object publish(Long docId, RagVersionActionCommand command) {
        return versionAction("/knowledge/documents/" + docId + "/publish", command);
    }

    public Object rollback(Long docId, RagVersionActionCommand command) {
        return versionAction("/knowledge/documents/" + docId + "/rollback", command);
    }

    public Object rebuild(RagKbRebuildCommand command) {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceId", TraceIdUtils.currentTraceId());
        payload.put("kbCode", command.kbCode());
        payload.put("kbName", command.kbName());
        payload.put("kbTypeCode", command.kbTypeCode());
        payload.put("knowledgeVersion", command.knowledgeVersion());
        payload.put("orgId", user.getOrgId());
        return ragAdminClient.post("/knowledge/rebuild", payload, TraceIdUtils.currentTraceId());
    }

    public Object ingestJobs() {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        return ragAdminClient.get("/knowledge/ingest-jobs", Map.of("org_id", user.getOrgId()), TraceIdUtils.currentTraceId());
    }

    public Object rebuildJobs(String kbCode) {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("kb_code", kbCode);
        query.put("org_id", user.getOrgId());
        return ragAdminClient.get("/knowledge/rebuild-jobs", query, TraceIdUtils.currentTraceId());
    }

    private Object versionAction(String path, RagVersionActionCommand command) {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceId", TraceIdUtils.currentTraceId());
        payload.put("versionNo", command.versionNo());
        payload.put("comment", command.comment());
        payload.put("operatorId", user.getUserId());
        payload.put("reviewerId", user.getUserId());
        payload.put("orgId", user.getOrgId());
        return ragAdminClient.post(path, payload, TraceIdUtils.currentTraceId());
    }
}
