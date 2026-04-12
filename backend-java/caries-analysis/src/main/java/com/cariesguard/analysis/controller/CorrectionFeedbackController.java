package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.app.CorrectionFeedbackAppService;
import com.cariesguard.analysis.interfaces.command.SubmitCorrectionFeedbackCommand;
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

@Tag(name = "医生修正", description = "医生修正反馈管理")
@RestController
@RequestMapping("/api/v1/analysis/corrections")
public class CorrectionFeedbackController {

    private final CorrectionFeedbackAppService correctionFeedbackAppService;

    public CorrectionFeedbackController(CorrectionFeedbackAppService correctionFeedbackAppService) {
        this.correctionFeedbackAppService = correctionFeedbackAppService;
    }

    @Operation(summary = "提交修正反馈")
    @PostMapping
    @RequirePermission("analysis:correct")
    public ApiResponse<CorrectionFeedbackVO> submitCorrectionFeedback(@Valid @RequestBody SubmitCorrectionFeedbackCommand command) {
        return ApiResponse.success(correctionFeedbackAppService.submit(command), TraceIdUtils.currentTraceId());
    }
}
