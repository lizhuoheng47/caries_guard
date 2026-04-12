package com.cariesguard.patient.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.patient.app.PatientCommandAppService;
import com.cariesguard.patient.app.PatientQueryAppService;
import com.cariesguard.patient.interfaces.command.CreatePatientCommand;
import com.cariesguard.patient.interfaces.command.UpdatePatientCommand;
import com.cariesguard.patient.interfaces.vo.PageResultVO;
import com.cariesguard.patient.interfaces.vo.PatientDetailVO;
import com.cariesguard.patient.interfaces.vo.PatientListItemVO;
import com.cariesguard.patient.interfaces.vo.PatientMutationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientCommandAppService patientCommandAppService;
    private final PatientQueryAppService patientQueryAppService;

    public PatientController(PatientCommandAppService patientCommandAppService,
                             PatientQueryAppService patientQueryAppService) {
        this.patientCommandAppService = patientCommandAppService;
        this.patientQueryAppService = patientQueryAppService;
    }

    @PostMapping
    @RequirePermission("patient:create")
    public ApiResponse<PatientMutationVO> createPatient(@Valid @RequestBody CreatePatientCommand command) {
        return ApiResponse.success(patientCommandAppService.createPatient(command), TraceIdUtils.currentTraceId());
    }

    @PutMapping("/{patientId}")
    @RequirePermission("patient:update")
    public ApiResponse<PatientMutationVO> updatePatient(@PathVariable Long patientId,
                                                        @Valid @RequestBody UpdatePatientCommand command) {
        return ApiResponse.success(patientCommandAppService.updatePatient(patientId, command), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/{patientId}")
    @RequirePermission("patient:view")
    public ApiResponse<PatientDetailVO> getPatient(@PathVariable Long patientId) {
        return ApiResponse.success(patientQueryAppService.getPatient(patientId), TraceIdUtils.currentTraceId());
    }

    @GetMapping
    @RequirePermission("patient:list")
    public ApiResponse<PageResultVO<PatientListItemVO>> pagePatients(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sourceCode,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(
                patientQueryAppService.pagePatients(pageNo, pageSize, keyword, sourceCode, status),
                TraceIdUtils.currentTraceId());
    }
}
