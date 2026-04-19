package com.cariesguard.report.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.report.app.RagEvalAppService;
import com.cariesguard.report.app.RagLogAppService;
import com.cariesguard.report.interfaces.command.RagEvalRunCommand;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
public class RagLogController {

    private final RagLogAppService ragLogAppService;
    private final RagEvalAppService ragEvalAppService;

    public RagLogController(RagLogAppService ragLogAppService, RagEvalAppService ragEvalAppService) {
        this.ragLogAppService = ragLogAppService;
        this.ragEvalAppService = ragEvalAppService;
    }

    @GetMapping("/logs/requests")
    @RequirePermission("report:view")
    public ApiResponse<Object> requests() {
        return ApiResponse.success(ragLogAppService.listRequests(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/logs/requests/{requestNo}")
    @RequirePermission("report:view")
    public ApiResponse<Object> requestDetail(@PathVariable String requestNo) {
        return ApiResponse.success(ragLogAppService.requestDetail(requestNo), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/logs/retrievals/{requestNo}")
    @RequirePermission("report:view")
    public ApiResponse<Object> retrievalLogs(@PathVariable String requestNo) {
        return ApiResponse.success(ragLogAppService.retrievalLogs(requestNo), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/logs/graph/{requestNo}")
    @RequirePermission("report:view")
    public ApiResponse<Object> graphLogs(@PathVariable String requestNo) {
        return ApiResponse.success(ragLogAppService.graphLogs(requestNo), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/eval/runs")
    @RequirePermission("report:view")
    public ApiResponse<Object> runs() {
        return ApiResponse.success(ragEvalAppService.runs(), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/eval/run")
    @RequirePermission("report:view")
    public ApiResponse<Object> run(@Valid @RequestBody RagEvalRunCommand command) {
        return ApiResponse.success(ragEvalAppService.run(command), TraceIdUtils.currentTraceId());
    }
}
