package com.cariesguard.system.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caries.competition")
public class CompetitionModeProperties {

    private boolean enabled = false;

    private List<String> hiddenPermissionPrefixes = List.of(
            "system:",
            "followup:",
            "report:template:");

    private List<String> hiddenPermissionCodes = List.of(
            "report:export",
            "dashboard:view");

    private List<String> hiddenRoutePaths = List.of(
            "/followups",
            "/dashboard");

    private List<String> forceVisibleRoutePaths = List.of(
            "/dashboard/model-runtime");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getHiddenPermissionPrefixes() {
        return hiddenPermissionPrefixes;
    }

    public void setHiddenPermissionPrefixes(List<String> hiddenPermissionPrefixes) {
        this.hiddenPermissionPrefixes = hiddenPermissionPrefixes == null ? List.of() : List.copyOf(hiddenPermissionPrefixes);
    }

    public List<String> getHiddenPermissionCodes() {
        return hiddenPermissionCodes;
    }

    public void setHiddenPermissionCodes(List<String> hiddenPermissionCodes) {
        this.hiddenPermissionCodes = hiddenPermissionCodes == null ? List.of() : List.copyOf(hiddenPermissionCodes);
    }

    public List<String> getHiddenRoutePaths() {
        return hiddenRoutePaths;
    }

    public void setHiddenRoutePaths(List<String> hiddenRoutePaths) {
        this.hiddenRoutePaths = hiddenRoutePaths == null ? List.of() : List.copyOf(hiddenRoutePaths);
    }

    public List<String> getForceVisibleRoutePaths() {
        return forceVisibleRoutePaths;
    }

    public void setForceVisibleRoutePaths(List<String> forceVisibleRoutePaths) {
        this.forceVisibleRoutePaths = forceVisibleRoutePaths == null ? List.of() : List.copyOf(forceVisibleRoutePaths);
    }
}
