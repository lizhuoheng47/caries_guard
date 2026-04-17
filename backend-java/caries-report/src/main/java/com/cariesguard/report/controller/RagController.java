package com.cariesguard.report.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.authorization.RequirePermission;
import com.cariesguard.report.app.RagAppService;
import com.cariesguard.report.interfaces.command.DoctorQaCommand;
import com.cariesguard.report.interfaces.command.PatientExplanationCommand;
import com.cariesguard.report.interfaces.vo.RagAnswerVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final RagAppService ragAppService;

    public RagController(RagAppService ragAppService) {
        this.ragAppService = ragAppService;
    }

    @PostMapping("/doctor-qa")
    @RequirePermission("report:view")
    public ApiResponse<RagAnswerVO> doctorQa(@Valid @RequestBody DoctorQaCommand command) {
        return ApiResponse.success(ragAppService.doctorQa(command), TraceIdUtils.currentTraceId());
    }

    @PostMapping("/patient-explanation")
    @RequirePermission("report:view")
    public ApiResponse<RagAnswerVO> patientExplanation(@Valid @RequestBody PatientExplanationCommand command) {
        return ApiResponse.success(ragAppService.patientExplanation(command), TraceIdUtils.currentTraceId());
    }
}
