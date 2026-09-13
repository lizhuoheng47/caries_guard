package com.cariesguard.system.interfaces.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ConfirmPasswordResetCommand {

    @NotBlank
    @Size(min = 3, max = 64)
    private String username;

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String verificationCode;

    @NotBlank
    @Size(min = 8, max = 72)
    private String newPassword;

    @NotBlank
    @Size(min = 8, max = 72)
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
