package com.cariesguard.system.app;

import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.system.domain.model.SystemLoginLogModel;
import com.cariesguard.system.domain.model.SystemUserAuthModel;
import com.cariesguard.system.domain.repository.SystemLoginLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAuditService {

    private final SystemLoginLogRepository systemLoginLogRepository;

    public LoginAuditService(SystemLoginLogRepository systemLoginLogRepository) {
        this.systemLoginLogRepository = systemLoginLogRepository;
    }

    public void recordSuccess(SystemUserAuthModel user, HttpServletRequest request) {
        systemLoginLogRepository.save(new SystemLoginLogModel(
                TraceIdUtils.currentTraceId(),
                user.username(),
                user.userId(),
                user.orgId(),
                "SUCCESS",
                extractClientIp(request),
                extractUserAgent(request),
                null,
                LocalDateTime.now()));
    }

    public void recordFailure(String username, HttpServletRequest request, String failureReason) {
        systemLoginLogRepository.save(new SystemLoginLogModel(
                TraceIdUtils.currentTraceId(),
                username,
                null,
                null,
                "FAILED",
                extractClientIp(request),
                extractUserAgent(request),
                failureReason,
                LocalDateTime.now()));
    }

    public void recordFailure(SystemUserAuthModel user, HttpServletRequest request, String failureReason) {
        systemLoginLogRepository.save(new SystemLoginLogModel(
                TraceIdUtils.currentTraceId(),
                user.username(),
                user.userId(),
                user.orgId(),
                "FAILED",
                extractClientIp(request),
                extractUserAgent(request),
                failureReason,
                LocalDateTime.now()));
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }
}
