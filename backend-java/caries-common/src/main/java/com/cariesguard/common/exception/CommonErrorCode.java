package com.cariesguard.common.exception;

public enum CommonErrorCode implements ErrorCode {

    INVALID_TOKEN("A0001", "Token is invalid"),
    FORBIDDEN("A0002", "Permission denied"),
    INVALID_CREDENTIALS("A0003", "Username or password is incorrect"),
    ACCOUNT_DISABLED("A0004", "User account is disabled"),
    AUTHENTICATION_REQUIRED("A0005", "Authentication is required"),
    VALIDATION_FAILED("B0001", "Request validation failed"),
    BUSINESS_ERROR("B9999", "Business processing failed"),
    EXTERNAL_SERVICE_ERROR("C0001", "External service invocation failed"),
    DATABASE_ERROR("D0001", "Database access failed"),
    SYSTEM_ERROR("E5000", "System internal error");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
