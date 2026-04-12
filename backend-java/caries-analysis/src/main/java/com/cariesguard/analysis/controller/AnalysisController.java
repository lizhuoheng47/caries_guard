package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.app.AnalysisTaskAppService;
import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalysisController {

    private final AnalysisTaskAppService analysisTaskAppService;

    public AnalysisController(AnalysisTaskAppService analysisTaskAppService) {
        this.analysisTaskAppService = analysisTaskAppService;
    }

    @PostMapping("/api/v1/cases/{caseId}/analysis")
    @RequirePermission("analysis:create")
    public ApiResponse<AnalysisTaskVO> createTask(@PathVariable Long caseId,
                                                  @Valid @RequestBody CreateAnalysisTaskCommand command) {
        return ApiResponse.success(analysisTaskAppService.createTask(caseId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/analysis/tasks/{taskId}")
    @RequirePermission("analysis:view")
    public ApiResponse<AnalysisTaskVO> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(analysisTaskAppService.getTask(taskId), TraceIdUtils.currentTraceId());
    }
}
