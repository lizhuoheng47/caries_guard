package com.cariesguard.system.app;

import com.cariesguard.common.exception.BusinessException;
import com.cariesguard.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class SystemOperationLogAspect {

    private final SystemOperLogService systemOperLogService;

    public SystemOperationLogAspect(SystemOperLogService systemOperLogService) {
        this.systemOperLogService = systemOperLogService;
    }

    @Around("@annotation(com.cariesguard.system.app.SystemOperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        SystemOperationLog annotation = ((MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getAnnotation(SystemOperationLog.class);
        HttpServletRequest request = currentRequest();
        try {
            Object result = joinPoint.proceed();
            systemOperLogService.record(annotation, request, true, "00000", null);
            return result;
        } catch (BusinessException exception) {
            systemOperLogService.record(annotation, request, false, exception.getCode(), exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            systemOperLogService.record(
                    annotation,
                    request,
                    false,
                    CommonErrorCode.SYSTEM_ERROR.code(),
                    exception.getMessage());
            throw exception;
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
