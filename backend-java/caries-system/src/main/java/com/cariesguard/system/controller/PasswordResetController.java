package com.cariesguard.system.controller;

import com.cariesguard.common.api.ApiResponse;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.system.app.PasswordResetAppService;
import com.cariesguard.system.interfaces.command.ConfirmPasswordResetCommand;
import com.cariesguard.system.interfaces.command.RequestPasswordResetCommand;
import com.cariesguard.system.interfaces.vo.PasswordResetConfirmVO;
import com.cariesguard.system.interfaces.vo.PasswordResetRequestVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetAppService passwordResetAppService;

    public PasswordResetController(PasswordResetAppService passwordResetAppService) {
        this.passwordResetAppService = passwordResetAppService;
    }

    @PostMapping("/request")
    public ApiResponse<PasswordResetRequestVO> request(
            @Valid @RequestBody RequestPasswordResetCommand command,
            HttpServletRequest request) {
        return ApiResponse.success(
                passwordResetAppService.requestReset(command, clientIp(request)),
                TraceIdUtils.currentTraceId());
    }

    @PostMapping("/confirm")
    public ApiResponse<PasswordResetConfirmVO> confirm(
            @Valid @RequestBody ConfirmPasswordResetCommand command) {
        return ApiResponse.success(
                passwordResetAppService.confirmReset(command),
                TraceIdUtils.currentTraceId());
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }
}
