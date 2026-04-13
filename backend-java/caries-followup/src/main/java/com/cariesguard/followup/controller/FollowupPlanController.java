package com.cariesguard.followup.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.followup.app.FollowupPlanAppService;
import com.cariesguard.followup.app.FollowupQueryService;
import com.cariesguard.followup.interfaces.command.CreateFollowupPlanCommand;
import com.cariesguard.followup.interfaces.vo.FollowupPlanVO;
import com.cariesguard.framework.security.authorization.RequirePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FollowupPlanController {

    private final FollowupPlanAppService followupPlanAppService;
    private final FollowupQueryService followupQueryService;

    public FollowupPlanController(FollowupPlanAppService followupPlanAppService,
                                  FollowupQueryService followupQueryService) {
        this.followupPlanAppService = followupPlanAppService;
        this.followupQueryService = followupQueryService;
    }

    @PostMapping("/api/v1/cases/{caseId}/followup/plans")
    @RequirePermission("followup:plan:create")
    public ApiResponse<FollowupPlanVO> createPlan(@PathVariable Long caseId,
                                                  @Valid @RequestBody CreateFollowupPlanCommand command) {
        return ApiResponse.success(followupPlanAppService.createPlan(caseId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/cases/{caseId}/followup/plans")
    @RequirePermission("followup:plan:view")
    public ApiResponse<List<FollowupPlanVO>> listCasePlans(@PathVariable Long caseId) {
        return ApiResponse.success(followupQueryService.listCasePlans(caseId), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/followup/plans/{planId}")
    @RequirePermission("followup:plan:view")
    public ApiResponse<FollowupPlanVO> getPlan(@PathVariable Long planId) {
        return ApiResponse.success(followupPlanAppService.getPlan(planId), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/api/v1/followup/plans/{planId}/cancel")
    @RequirePermission("followup:plan:manage")
    public ApiResponse<Void> cancelPlan(@PathVariable Long planId) {
        followupPlanAppService.cancelPlan(planId);
        return ApiResponse.success(null, TraceIdUtils.currentTraceId());
    }

    @PostMapping("/api/v1/followup/plans/{planId}/close")
    @RequirePermission("followup:plan:manage")
    public ApiResponse<Void> closePlan(@PathVariable Long planId) {
        followupPlanAppService.closePlan(planId);
        return ApiResponse.success(null, TraceIdUtils.currentTraceId());
    }
}
