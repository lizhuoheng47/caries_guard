package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.app.ReviewBffAppService;
import com.cariesguard.analysis.interfaces.vo.AnalysisTaskDetailVO;
import com.cariesguard.analysis.interfaces.vo.ReviewWorkbenchVO;
import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "Review", description = "Doctor review workbench")
@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    private final ReviewBffAppService reviewBffAppService;

    public ReviewController(ReviewBffAppService reviewBffAppService) {
        this.reviewBffAppService = reviewBffAppService;
    }

    @Operation(summary = "Get review queue")
    @GetMapping("/queue")
    @RequirePermission("analysis:correct")
    public ApiResponse<List<AnalysisTaskDetailVO>> getReviewQueue() {
        return ApiResponse.success(reviewBffAppService.getReviewQueue(), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Get review task view (BFF)")
    @GetMapping("/tasks/{taskId}/view")
    @RequirePermission("analysis:correct")
    public ApiResponse<ReviewWorkbenchVO> getReviewTaskView(@PathVariable Long taskId) {
        return ApiResponse.success(reviewBffAppService.getReviewTaskView(taskId), TraceIdUtils.currentTraceId());
    }
}
