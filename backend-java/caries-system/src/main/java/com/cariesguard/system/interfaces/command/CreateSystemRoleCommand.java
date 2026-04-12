package com.cariesguard.system.interfaces.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateSystemRoleCommand {

    @NotBlank
    private String roleCode;

    @NotBlank
    private String roleName;

    @NotNull
    private Integer roleSort;

    @NotBlank
    private String dataScopeCode;

    private String status;

    private String remark;

    @NotEmpty
    private List<Long> menuIds;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Integer getRoleSort() {
        return roleSort;
    }

    public void setRoleSort(Integer roleSort) {
        this.roleSort = roleSort;
    }

    public String getDataScopeCode() {
        return dataScopeCode;
    }

    public void setDataScopeCode(String dataScopeCode) {
        this.dataScopeCode = dataScopeCode;
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

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }
}
