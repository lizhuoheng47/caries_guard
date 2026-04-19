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

    @GetMapping("/logs/fusion/{requestNo}")
    @RequirePermission("report:view")
    public ApiResponse<Object> fusionLogs(@PathVariable String requestNo) {
        return ApiResponse.success(ragLogAppService.fusionLogs(requestNo), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/logs/rerank/{requestNo}")
    @RequirePermission("report:view")
    public ApiResponse<Object> rerankLogs(@PathVariable String requestNo) {
        return ApiResponse.success(ragLogAppService.rerankLogs(requestNo), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/logs/llm/{requestNo}")
    @RequirePermission("report:view")
    public ApiResponse<Object> llmLogs(@PathVariable String requestNo) {
        return ApiResponse.success(ragLogAppService.llmLogs(requestNo), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/eval/datasets")
    @RequirePermission("report:view")
    public ApiResponse<Object> datasets() {
        return ApiResponse.success(ragEvalAppService.datasets(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/eval/datasets/{datasetId}")
    @RequirePermission("report:view")
    public ApiResponse<Object> datasetDetail(@PathVariable Long datasetId) {
        return ApiResponse.success(ragEvalAppService.datasetDetail(datasetId), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/eval/runs")
    @RequirePermission("report:view")
    public ApiResponse<Object> runs() {
        return ApiResponse.success(ragEvalAppService.runs(), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/eval/runs/{runNo}")
    @RequirePermission("report:view")
    public ApiResponse<Object> runDetail(@PathVariable String runNo) {
        return ApiResponse.success(ragEvalAppService.runDetail(runNo), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/eval/runs/{runNo}/results")
    @RequirePermission("report:view")
    public ApiResponse<Object> runResults(@PathVariable String runNo) {
        return ApiResponse.success(ragEvalAppService.runResults(runNo), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/eval/run")
    @RequirePermission("report:view")
    public ApiResponse<Object> run(@Valid @RequestBody RagEvalRunCommand command) {
        return ApiResponse.success(ragEvalAppService.run(command), TraceIdUtils.currentTraceId());
    }
}
