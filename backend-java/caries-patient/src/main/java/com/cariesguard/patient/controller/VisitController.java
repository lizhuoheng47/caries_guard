package com.cariesguard.patient.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.patient.app.VisitCommandAppService;
import com.cariesguard.patient.app.VisitQueryAppService;
import com.cariesguard.patient.interfaces.command.CreateVisitCommand;
import com.cariesguard.patient.interfaces.vo.PageResultVO;
import com.cariesguard.patient.interfaces.vo.VisitDetailVO;
import com.cariesguard.patient.interfaces.vo.VisitListItemVO;
import com.cariesguard.patient.interfaces.vo.VisitMutationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visits")
public class VisitController {

    private final VisitCommandAppService visitCommandAppService;
    private final VisitQueryAppService visitQueryAppService;

    public VisitController(VisitCommandAppService visitCommandAppService,
                           VisitQueryAppService visitQueryAppService) {
        this.visitCommandAppService = visitCommandAppService;
        this.visitQueryAppService = visitQueryAppService;
    }

    @PostMapping
    @RequirePermission("visit:create")
    public ApiResponse<VisitMutationVO> createVisit(@Valid @RequestBody CreateVisitCommand command) {
        return ApiResponse.success(visitCommandAppService.createVisit(command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/{visitId}")
    @RequirePermission("visit:view")
    public ApiResponse<VisitDetailVO> getVisit(@PathVariable Long visitId) {
        return ApiResponse.success(visitQueryAppService.getVisit(visitId), TraceIdUtils.currentTraceId());
    }

    @GetMapping
    @RequirePermission("visit:list")
    public ApiResponse<PageResultVO<VisitListItemVO>> pageVisits(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorUserId,
            @RequestParam(required = false) String visitTypeCode) {
        return ApiResponse.success(
                visitQueryAppService.pageVisits(pageNo, pageSize, patientId, doctorUserId, visitTypeCode),
                TraceIdUtils.currentTraceId());
    }
}
