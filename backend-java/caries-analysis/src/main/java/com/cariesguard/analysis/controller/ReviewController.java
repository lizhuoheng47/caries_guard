package com.cariesguard.analysis.controller;

import com.cariesguard.analysis.app.ReviewBffAppService;
import com.cariesguard.analysis.app.DoctorReviewAppService;
import com.cariesguard.analysis.interfaces.command.SaveReviewDraftCommand;
import com.cariesguard.analysis.interfaces.vo.CorrectionFeedbackVO;
import com.cariesguard.analysis.interfaces.vo.ReviewDraftVO;
import com.cariesguard.analysis.interfaces.query.ReviewQueueQuery;
import com.cariesguard.analysis.interfaces.vo.ReviewQueuePageVO;
import com.cariesguard.analysis.interfaces.vo.ReviewTaskDetailVO;
import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@Tag(name = "Review", description = "Doctor review workbench")
@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    private final ReviewBffAppService reviewBffAppService;
    private final DoctorReviewAppService doctorReviewAppService;

    public ReviewController(ReviewBffAppService reviewBffAppService,
                            DoctorReviewAppService doctorReviewAppService) {
        this.reviewBffAppService = reviewBffAppService;
        this.doctorReviewAppService = doctorReviewAppService;
    }

    @Operation(summary = "Get review queue")
    @GetMapping("/queue")
    @RequirePermission("analysis:correct")
    public ApiResponse<ReviewQueuePageVO> getReviewQueue(ReviewQueueQuery query) {
        return ApiResponse.success(reviewBffAppService.getReviewQueue(query), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Get review task view (BFF)")
    @GetMapping("/tasks/{taskIdentifier}")
    @RequirePermission("analysis:correct")
    public ApiResponse<ReviewTaskDetailVO> getReviewTaskView(@PathVariable String taskIdentifier) {
        return ApiResponse.success(reviewBffAppService.getReviewTaskDetail(taskIdentifier), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Get review task view alias (BFF)")
    @GetMapping("/tasks/{taskIdentifier}/view")
    @RequirePermission("analysis:correct")
    public ApiResponse<ReviewTaskDetailVO> getReviewTaskViewAlias(@PathVariable String taskIdentifier) {
        return ApiResponse.success(reviewBffAppService.getReviewTaskDetail(taskIdentifier), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Save doctor review draft")
    @PutMapping("/tasks/{taskIdentifier}/draft")
    @RequirePermission("analysis:correct")
    public ApiResponse<ReviewDraftVO> saveDraft(@PathVariable String taskIdentifier,
                                                 @Valid @RequestBody SaveReviewDraftCommand command) {
        return ApiResponse.success(doctorReviewAppService.saveDraft(taskIdentifier, command), TraceIdUtils.currentTraceId());
    }

    @Operation(summary = "Submit doctor review")
    @PostMapping("/tasks/{taskIdentifier}/submit")
    @RequirePermission("analysis:correct")
    public ApiResponse<CorrectionFeedbackVO> submitReview(@PathVariable String taskIdentifier,
                                                           @Valid @RequestBody SaveReviewDraftCommand command) {
        return ApiResponse.success(doctorReviewAppService.submit(taskIdentifier, command), TraceIdUtils.currentTraceId());
    }
}
