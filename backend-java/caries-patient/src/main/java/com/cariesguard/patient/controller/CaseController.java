package com.cariesguard.patient.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.patient.app.CaseClinicalRecordAppService;
import com.cariesguard.patient.app.CaseCommandAppService;
import com.cariesguard.patient.app.CaseQueryAppService;
import com.cariesguard.patient.interfaces.command.CaseStatusTransitionCommand;
import com.cariesguard.patient.interfaces.command.CreateCaseCommand;
import com.cariesguard.patient.interfaces.command.SaveCaseDiagnosesCommand;
import com.cariesguard.patient.interfaces.command.SaveCaseToothRecordsCommand;
import com.cariesguard.patient.interfaces.vo.CaseDetailVO;
import com.cariesguard.patient.interfaces.vo.CaseDiagnosisMutationVO;
import com.cariesguard.patient.interfaces.vo.CaseListItemVO;
import com.cariesguard.patient.interfaces.vo.CaseMutationVO;
import com.cariesguard.patient.interfaces.vo.CaseStatusTransitionVO;
import com.cariesguard.patient.interfaces.vo.CaseToothRecordMutationVO;
import com.cariesguard.patient.interfaces.vo.PageResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {

    private final CaseCommandAppService caseCommandAppService;
    private final CaseQueryAppService caseQueryAppService;
    private final CaseClinicalRecordAppService caseClinicalRecordAppService;

    public CaseController(CaseCommandAppService caseCommandAppService,
                          CaseQueryAppService caseQueryAppService,
                          CaseClinicalRecordAppService caseClinicalRecordAppService) {
        this.caseCommandAppService = caseCommandAppService;
        this.caseQueryAppService = caseQueryAppService;
        this.caseClinicalRecordAppService = caseClinicalRecordAppService;
    }

    @PostMapping
    @RequirePermission("case:create")
    public ApiResponse<CaseMutationVO> createCase(@Valid @RequestBody CreateCaseCommand command) {
        return ApiResponse.success(caseCommandAppService.createCase(command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/{caseId}")
    @RequirePermission("case:view")
    public ApiResponse<CaseDetailVO> getCase(@PathVariable Long caseId) {
        return ApiResponse.success(caseQueryAppService.getCase(caseId), TraceIdUtils.currentTraceId());
    }

    @GetMapping
    @RequirePermission("case:list")
    public ApiResponse<PageResultVO<CaseListItemVO>> pageCases(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String caseStatusCode,
            @RequestParam(required = false) Long attendingDoctorId) {
        return ApiResponse.success(
                caseQueryAppService.pageCases(pageNo, pageSize, patientId, caseStatusCode, attendingDoctorId),
                TraceIdUtils.currentTraceId());
    }

    @PostMapping("/{caseId}/diagnoses")
    @RequirePermission("case:update")
    public ApiResponse<CaseDiagnosisMutationVO> saveDiagnoses(@PathVariable Long caseId,
                                                              @Valid @RequestBody SaveCaseDiagnosesCommand command) {
        return ApiResponse.success(caseClinicalRecordAppService.saveDiagnoses(caseId, command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/{caseId}/tooth-records")
    @RequirePermission("case:update")
    public ApiResponse<CaseToothRecordMutationVO> saveToothRecords(@PathVariable Long caseId,
                                                                   @Valid @RequestBody SaveCaseToothRecordsCommand command) {
        return ApiResponse.success(caseClinicalRecordAppService.saveToothRecords(caseId, command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/{caseId}/status-transition")
    @RequirePermission("case:transition")
    public ApiResponse<CaseStatusTransitionVO> transitionStatus(@PathVariable Long caseId,
                                                                @Valid @RequestBody CaseStatusTransitionCommand command) {
        return ApiResponse.success(caseCommandAppService.transitionStatus(caseId, command), TraceIdUtils.currentTraceId());
    }
}
