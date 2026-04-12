package com.cariesguard.system.interfaces.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class UpdateSystemDataPermissionRuleCommand {

    @NotNull
    private Long roleId;

    @NotBlank
    private String moduleCode;

    @NotBlank
    private String scopeTypeCode;

    private List<Long> customDeptIds = List.of();

    private Map<String, Object> columnMaskPolicy = Map.of();

    @NotBlank
    private String status;

    private String remark;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getScopeTypeCode() {
        return scopeTypeCode;
    }

    public void setScopeTypeCode(String scopeTypeCode) {
        this.scopeTypeCode = scopeTypeCode;
    }

    public List<Long> getCustomDeptIds() {
        return customDeptIds;
    }

    public void setCustomDeptIds(List<Long> customDeptIds) {
        this.customDeptIds = customDeptIds;
    }

    public Map<String, Object> getColumnMaskPolicy() {
        return columnMaskPolicy;
    }

    public void setColumnMaskPolicy(Map<String, Object> columnMaskPolicy) {
        this.columnMaskPolicy = columnMaskPolicy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
