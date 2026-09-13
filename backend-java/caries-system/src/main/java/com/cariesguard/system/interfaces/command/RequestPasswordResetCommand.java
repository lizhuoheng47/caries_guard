package com.cariesguard.system.interfaces.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RequestPasswordResetCommand {

    @NotBlank
    @Size(min = 3, max = 64)
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
