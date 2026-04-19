package com.cariesguard.report.app;

import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.report.infrastructure.client.HttpRagAdminClient;
import com.cariesguard.report.interfaces.command.RagEvalRunCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RagEvalAppService {

    private final HttpRagAdminClient ragAdminClient;

    public RagEvalAppService(HttpRagAdminClient ragAdminClient) {
        this.ragAdminClient = ragAdminClient;
    }

    public Object runs() {
        return ragAdminClient.get("/eval/runs", Map.of(), TraceIdUtils.currentTraceId());
    }

    public Object run(RagEvalRunCommand command) {
        AuthenticatedUser user = SecurityContextUtils.currentUser();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("datasetId", command.datasetId());
        payload.put("orgId", user.getOrgId());
        payload.put("operatorId", user.getUserId());
        payload.put("traceId", TraceIdUtils.currentTraceId());
        return ragAdminClient.post("/eval/run", payload, TraceIdUtils.currentTraceId());
    }
}
