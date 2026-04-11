package com.cariesguard.system.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.system.app.AuthAppService;
import com.cariesguard.system.app.SystemOperationLog;
import com.cariesguard.system.interfaces.command.LoginCommand;
import com.cariesguard.system.interfaces.vo.CurrentUserVO;
import com.cariesguard.system.interfaces.vo.LoginTokenVO;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthAppService authAppService;

    public AuthController(AuthAppService authAppService) {
        this.authAppService = authAppService;
    }

    @PostMapping("/login")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "LOGIN", operationName = "User login")
    public ApiResponse<LoginTokenVO> login(@Valid @RequestBody LoginCommand command, HttpServletRequest request) {
        return ApiResponse.success(authAppService.login(command, request), TraceIdUtils.currentTraceId());
    }

    @GetMapping("/me")
    @SystemOperationLog(moduleCode = "SYSTEM", operationTypeCode = "QUERY", operationName = "Current user profile")
    public ApiResponse<CurrentUserVO> currentUser() {
        return ApiResponse.success(authAppService.currentUser(), TraceIdUtils.currentTraceId());
    }
}
