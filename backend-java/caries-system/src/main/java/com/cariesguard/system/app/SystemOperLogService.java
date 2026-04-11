package com.cariesguard.system.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import com.cariesguard.common.util.TraceIdUtils;
import com.cariesguard.framework.security.context.SecurityContextUtils;
import com.cariesguard.framework.security.principal.AuthenticatedUser;
import com.cariesguard.system.domain.model.SystemOperLogModel;
import com.cariesguard.system.domain.repository.SystemOperLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class SystemOperLogService {

    private final SystemOperLogRepository systemOperLogRepository;

    public SystemOperLogService(SystemOperLogRepository systemOperLogRepository) {
        this.systemOperLogRepository = systemOperLogRepository;
    }

    public void record(SystemOperationLog annotation,
                       HttpServletRequest request,
                       boolean success,
                       String resultCode,
                       String errorMessage) {
        AuthenticatedUser currentUser = tryGetCurrentUser();
        systemOperLogRepository.save(new SystemOperLogModel(
                TraceIdUtils.currentTraceId(),
                annotation.moduleCode(),
                annotation.operationTypeCode(),
                annotation.operationName(),
                request == null ? null : request.getRequestURI(),
                request == null ? null : request.getMethod(),
                null,
                currentUser == null ? null : currentUser.getUserId(),
                currentUser == null ? null : currentUser.getOrgId(),
                success,
                resultCode,
                errorMessage,
                LocalDateTime.now()));
    }

    private AuthenticatedUser tryGetCurrentUser() {
        try {
            return SecurityContextUtils.currentUser();
        } catch (BusinessException exception) {
            if (CommonErrorCode.AUTHENTICATION_REQUIRED.code().equals(exception.getCode())) {
                return null;
            }
            throw exception;
        }
    }
}
