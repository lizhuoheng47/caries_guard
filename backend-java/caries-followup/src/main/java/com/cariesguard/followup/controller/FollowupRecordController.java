package com.cariesguard.followup.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.followup.app.FollowupQueryService;
import com.cariesguard.followup.app.FollowupRecordAppService;
import com.cariesguard.followup.interfaces.command.CreateFollowupRecordCommand;
import com.cariesguard.followup.interfaces.vo.FollowupRecordVO;
import com.cariesguard.framework.security.authorization.RequirePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FollowupRecordController {

    private final FollowupRecordAppService followupRecordAppService;
    private final FollowupQueryService followupQueryService;

    public FollowupRecordController(FollowupRecordAppService followupRecordAppService,
                                    FollowupQueryService followupQueryService) {
        this.followupRecordAppService = followupRecordAppService;
        this.followupQueryService = followupQueryService;
    }

    @PostMapping("/api/v1/followup/records")
    @RequirePermission("followup:record:create")
    public ApiResponse<FollowupRecordVO> addRecord(@Valid @RequestBody CreateFollowupRecordCommand command) {
        return ApiResponse.success(followupRecordAppService.addRecord(command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/cases/{caseId}/followup/records")
    @RequirePermission("followup:record:view")
    public ApiResponse<List<FollowupRecordVO>> listCaseRecords(@PathVariable Long caseId) {
        return ApiResponse.success(followupQueryService.listCaseRecords(caseId), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/api/v1/followup/tasks/{taskId}/records")
    @RequirePermission("followup:record:view")
    public ApiResponse<List<FollowupRecordVO>> listTaskRecords(@PathVariable Long taskId) {
        return ApiResponse.success(followupRecordAppService.listByTask(taskId), TraceIdUtils.currentTraceId());
    }
}
