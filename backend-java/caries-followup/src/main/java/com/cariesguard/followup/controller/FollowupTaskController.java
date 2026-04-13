package com.cariesguard.followup.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.followup.app.FollowupQueryService;
import com.cariesguard.followup.app.FollowupTaskAppService;
import com.cariesguard.followup.interfaces.command.CreateFollowupTaskCommand;
import com.cariesguard.followup.interfaces.command.UpdateFollowupTaskStatusCommand;
import com.cariesguard.followup.interfaces.vo.FollowupTaskVO;
import com.cariesguard.framework.security.authorization.RequirePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FollowupTaskController {

    private final FollowupTaskAppService followupTaskAppService;
    private final FollowupQueryService followupQueryService;

    public FollowupTaskController(FollowupTaskAppService followupTaskAppService,
                                  FollowupQueryService followupQueryService) {
        this.followupTaskAppService = followupTaskAppService;
        this.followupQueryService = followupQueryService;
    }

    @PostMapping("/api/v1/cases/{caseId}/followup/tasks")
    @RequirePermission("followup:task:create")
    public ApiResponse<FollowupTaskVO> createTask(@PathVariable Long caseId,
                                                  @Valid @RequestBody CreateFollowupTaskCommand command) {
        return ApiResponse.success(followupTaskAppService.createTask(caseId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/cases/{caseId}/followup/tasks")
    @RequirePermission("followup:task:view")
    public ApiResponse<List<FollowupTaskVO>> listCaseTasks(@PathVariable Long caseId) {
        return ApiResponse.success(followupQueryService.listCaseTasks(caseId), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/followup/tasks/{taskId}")
    @RequirePermission("followup:task:view")
    public ApiResponse<FollowupTaskVO> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(followupTaskAppService.getTask(taskId), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/api/v1/followup/tasks/{taskId}/status")
    @RequirePermission("followup:task:manage")
    public ApiResponse<FollowupTaskVO> updateTaskStatus(@PathVariable Long taskId,
                                                        @Valid @RequestBody UpdateFollowupTaskStatusCommand command) {
        return ApiResponse.success(followupTaskAppService.updateTaskStatus(taskId, command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/api/v1/followup/tasks/{taskId}/assign")
    @RequirePermission("followup:task:manage")
    public ApiResponse<Void> assignTask(@PathVariable Long taskId,
                                        @RequestParam Long assigneeUserId) {
        followupTaskAppService.assignTask(taskId, assigneeUserId);
        return ApiResponse.success(null, TraceIdUtils.currentTraceId());
    }
}
