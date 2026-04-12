package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.app.AnalysisTaskAppService;
import com.cariesguard.analysis.app.CorrectionFeedbackAppService;
import com.cariesguard.analysis.interfaces.command.CreateAnalysisTaskCommand;
import com.cariesguard.analysis.interfaces.command.SubmitCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackVO;
import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Case-centric route aliases for analysis operations (D1).
 * These delegate to the same AppService methods as the resource-centric routes.
 */
@Tag(name = "病例分析", description = "以病例为中心的分析操作别名")
@RestController
@RequestMapping("/api/v1/cases/{caseId}")
public class CaseAnalysisAliasController {

    private final AnalysisTaskAppService analysisTaskAppService;
    private final CorrectionFeedbackAppService correctionFeedbackAppService;

    public CaseAnalysisAliasController(AnalysisTaskAppService analysisTaskAppService,
                                        CorrectionFeedbackAppService correctionFeedbackAppService) {
        this.analysisTaskAppService = analysisTaskAppService;
        this.correctionFeedbackAppService = correctionFeedbackAppService;
    }

    @Operation(summary = "创建分析任务（病例入口）")
    @PostMapping("/analysis")
    @RequirePermission("analysis:create")
    public ApiResponse<AnalysisTaskVO> createAnalysisFromCase(@PathVariable Long caseId,
                                                               @Valid @RequestBody CreateAnalysisTaskCommand command) {
        // Override caseId from path variable for consistency
        CreateAnalysisTaskCommand resolved = new CreateAnalysisTaskCommand(
                caseId, command.patientId(), command.forceRetryFlag(), command.taskTypeCode(), command.remark());
        return ApiResponse.success(analysisTaskAppService.createTask(resolved), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "提交修正反馈（病例入口）")
    @PostMapping("/corrections")
    @RequirePermission("analysis:correct")
    public ApiResponse<CorrectionFeedbackVO> submitCorrectionFromCase(@PathVariable Long caseId,
                                                                       @Valid @RequestBody SubmitCorrectionFeedbackCommand command) {
        SubmitCorrectionFeedbackCommand resolved = new SubmitCorrectionFeedbackCommand(
                caseId, command.diagnosisId(), command.sourceImageId(), command.feedbackTypeCode(),
                command.originalInferenceJson(), command.correctedTruthJson());
        return ApiResponse.success(correctionFeedbackAppService.submit(resolved), TraceIdUtils.currentTraceId());
    }
}
