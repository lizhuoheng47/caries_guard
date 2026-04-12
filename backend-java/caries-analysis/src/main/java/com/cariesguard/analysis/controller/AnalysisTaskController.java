package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.app.AnalysisQueryAppService;
import com.cariesguard.analysis.app.AnalysisTaskAppService;
import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.command.RetryAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.query.AnalysisTaskPageQuery;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskPageVO;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "分析任务", description = "AI 分析任务管理")
@RestController
@RequestMapping("/api/v1/analysis/tasks")
public class AnalysisTaskController {

    private final AnalysisTaskAppService analysisTaskAppService;
    private final AnalysisQueryAppService analysisQueryAppService;

    public AnalysisTaskController(AnalysisTaskAppService analysisTaskAppService,
                                  AnalysisQueryAppService analysisQueryAppService) {
        this.analysisTaskAppService = analysisTaskAppService;
        this.analysisQueryAppService = analysisQueryAppService;
    }

    @Operation(summary = "创建分析任务")
    @PostMapping
    @RequirePermission("analysis:create")
    public ApiResponse<AnalysisTaskVO> createAnalysisTask(@Valid @RequestBody CreateAnalysisTaskCommand command) {
        return ApiResponse.success(analysisTaskAppService.createTask(command), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "重试失败任务")
    @PostMapping("/retry")
    @RequirePermission("analysis:create")
    public ApiResponse<AnalysisTaskVO> retryAnalysisTask(@Valid @RequestBody RetryAnalysisTaskCommand command) {
        return ApiResponse.success(analysisTaskAppService.retryTask(command), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "查询任务详情")
    @GetMapping("/{taskId}")
    @RequirePermission("analysis:view")
    public ApiResponse<AnalysisTaskDetailVO> getAnalysisTaskDetail(@PathVariable Long taskId) {
        return ApiResponse.success(analysisQueryAppService.getTaskDetail(taskId), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "分页查询任务")
    @GetMapping
    @RequirePermission("analysis:view")
    public ApiResponse<AnalysisTaskPageVO> pageAnalysisTasks(AnalysisTaskPageQuery query) {
        return ApiResponse.success(analysisQueryAppService.pageTasks(query), TraceIdUtils.currentTraceId());
    }
}
