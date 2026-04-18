package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.app.CorrectionFeedbackAppService;
import com.cariesguard.analysis.interfaces.command.ExportCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.command.ReviewCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.command.SubmitCorrectionFeedbackCommand;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackExportVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackReviewVO;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackVO;
import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Correction Feedback", description = "Doctor correction feedback governance")
@RestController
@RequestMapping("/api/v1/analysis/corrections")
public class CorrectionFeedbackController {

    private final CorrectionFeedbackAppService correctionFeedbackAppService;

    public CorrectionFeedbackController(CorrectionFeedbackAppService correctionFeedbackAppService) {
        this.correctionFeedbackAppService = correctionFeedbackAppService;
    }

    @Operation(summary = "Submit correction feedback")
    @PostMapping
    @RequirePermission("analysis:correct")
    public ApiResponse<CorrectionFeedbackVO> submitCorrectionFeedback(@Valid @RequestBody SubmitCorrectionFeedbackCommand command) {
        return ApiResponse.success(correctionFeedbackAppService.submit(command), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Export training correction candidates")
    @PostMapping("/training-candidates/export")
    @RequirePermission("analysis:correct")
    public ApiResponse<CorrectionFeedbackExportVO> exportTrainingCandidates(
            @Valid @RequestBody(required = false) ExportCorrectionFeedbackCommand command) {
        return ApiResponse.success(correctionFeedbackAppService.exportTrainingCandidates(command), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Review correction feedback")
    @PostMapping("/review")
    @RequirePermission("analysis:correct")
    public ApiResponse<CorrectionFeedbackReviewVO> reviewCorrectionFeedback(
            @Valid @RequestBody ReviewCorrectionFeedbackCommand command) {
        return ApiResponse.success(correctionFeedbackAppService.review(command), TraceIdUtils.currentTraceId());
    }
}
