package com.cariesguard.system.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caries.competition")
public class CompetitionModeProperties {

    private boolean enabled = false;

    /**
     * Permission prefixes to hide. E.g., "system:" will hide any permission code starting with "system:".
     */
    private List<String> hiddenPermissionPrefixes = List.of(
            "system:",
            "visit:",
            "followup:",
            "report:template:");

    /**
     * Exact permission codes to hide. E.g., "report:export".
     */
    private List<String> hiddenPermissionCodes = List.of(
            "report:export",
            "dashboard:view");

    /**
     * Route prefixes to hide. E.g., "/visits" will hide "/visits", "/visits/1", etc.
     */
    private List<String> hiddenRoutePrefixes = List.of(
            "/visits",
            "/followups",
            "/dashboard");

    /**
     * Exact route paths to hide completely.
     */
    private List<String> hiddenRoutePaths = List.of();

    /**
     * Route prefixes to force visible overriding hidden match. E.g., "/dashboard/model-runtime".
     */
    private List<String> forceVisibleRoutePrefixes = List.of(
            "/dashboard/model-runtime");

    /**
     * Exact route paths to force visible.
     */
    private List<String> forceVisibleRoutePaths = List.of();

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

    public List<String> getHiddenRoutePrefixes() {
        return hiddenRoutePrefixes;
    }

    public void setHiddenRoutePrefixes(List<String> hiddenRoutePrefixes) {
        this.hiddenRoutePrefixes = hiddenRoutePrefixes == null ? List.of() : List.copyOf(hiddenRoutePrefixes);
    }

    public List<String> getHiddenRoutePaths() {
        return hiddenRoutePaths;
    }

    public void setHiddenRoutePaths(List<String> hiddenRoutePaths) {
        this.hiddenRoutePaths = hiddenRoutePaths == null ? List.of() : List.copyOf(hiddenRoutePaths);
    }

    public List<String> getForceVisibleRoutePrefixes() {
        return forceVisibleRoutePrefixes;
    }

    public void setForceVisibleRoutePrefixes(List<String> forceVisibleRoutePrefixes) {
        this.forceVisibleRoutePrefixes = forceVisibleRoutePrefixes == null ? List.of() : List.copyOf(forceVisibleRoutePrefixes);
    }

    public List<String> getForceVisibleRoutePaths() {
        return forceVisibleRoutePaths;
    }

    public void setForceVisibleRoutePaths(List<String> forceVisibleRoutePaths) {
        this.forceVisibleRoutePaths = forceVisibleRoutePaths == null ? List.of() : List.copyOf(forceVisibleRoutePaths);
    }
}
