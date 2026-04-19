package com.cariesguard.report.app;

import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.infrastructure.client.HttpRagAdminClient;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RagLogAppService {

    private final HttpRagAdminClient ragAdminClient;

    public RagLogAppService(HttpRagAdminClient ragAdminClient) {
        this.ragAdminClient = ragAdminClient;
    }

    public Object listRequests() {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        return ragAdminClient.get("/logs/requests", Map.of("org_id", user.getOrgId()), TraceIdUtils.currentTraceId());
    }

    @SuppressWarnings("unchecked")
    public Object requestDetail(String requestNo) {
        return ragAdminClient.get("/logs/requests/" + requestNo, Map.of(), TraceIdUtils.currentTraceId());
    }

    @SuppressWarnings("unchecked")
    public Object retrievalLogs(String requestNo) {
        Object detail = requestDetail(requestNo);
        return detail instanceof Map<?, ?> map ? map.get("retrievalLogs") : List.of();
    }

    @SuppressWarnings("unchecked")
    public Object graphLogs(String requestNo) {
        Object detail = requestDetail(requestNo);
        return detail instanceof Map<?, ?> map ? map.get("graphLogs") : List.of();
    }
}
