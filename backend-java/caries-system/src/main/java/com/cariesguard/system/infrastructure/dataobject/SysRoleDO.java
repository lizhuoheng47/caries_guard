package com.cariesguard.system.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_role")
public class SysRoleDO {

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer roleSort;
    private String dataScopeCode;
    private String isBuiltin;
    private Long orgId;
    private String status;
    private Long deletedFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getIsBuiltin() {
        return isBuiltin;
    }

    public void setIsBuiltin(String isBuiltin) {
        this.isBuiltin = isBuiltin;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDeletedFlag() {
        return deletedFlag;
    }

    public void setDeletedFlag(Long deletedFlag) {
        this.deletedFlag = deletedFlag;
    }
}
