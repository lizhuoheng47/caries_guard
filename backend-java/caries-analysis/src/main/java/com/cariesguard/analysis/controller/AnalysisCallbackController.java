package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.app.AnalysisCallbackAppService;
import com.cariesguard.analysis.interfaces.vo.AnalysisCallbackAckVO;
import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 回调", description = "AI 服务回调接口（内部）")
@RestController
@RequestMapping("/api/v1/internal/ai/callbacks")
public class AnalysisCallbackController {

    private final AnalysisCallbackAppService analysisCallbackAppService;

    public AnalysisCallbackController(AnalysisCallbackAppService analysisCallbackAppService) {
        this.analysisCallbackAppService = analysisCallbackAppService;
    }

    @Operation(summary = "接收 AI 分析结果回调")
    @PostMapping(value = "/analysis-result", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<AnalysisCallbackAckVO> receiveAnalysisResultCallback(
            @RequestHeader("X-AI-Timestamp") String timestamp,
            @RequestHeader("X-AI-Signature") String signature,
            @RequestBody String rawBody) {
        return ApiResponse.success(
                analysisCallbackAppService.handleResultCallback(rawBody, timestamp, signature),
                TraceIdUtils.currentTraceId());
    }
}
