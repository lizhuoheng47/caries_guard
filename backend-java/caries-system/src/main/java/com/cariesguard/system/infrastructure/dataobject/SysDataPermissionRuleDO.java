package com.cariesguard.system.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_data_permission_rule")
public class SysDataPermissionRuleDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long roleId;
    private String moduleCode;
    private String scopeTypeCode;
    private String deptIdsJson;
    private String selfOnlyFlag;
    private String columnMaskPolicyJson;
    private Long orgId;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Long deletedFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getDeptIdsJson() {
        return deptIdsJson;
    }

    public void setDeptIdsJson(String deptIdsJson) {
        this.deptIdsJson = deptIdsJson;
    }

    public String getSelfOnlyFlag() {
        return selfOnlyFlag;
    }

    public void setSelfOnlyFlag(String selfOnlyFlag) {
        this.selfOnlyFlag = selfOnlyFlag;
    }

    public String getColumnMaskPolicyJson() {
        return columnMaskPolicyJson;
    }

    public void setColumnMaskPolicyJson(String columnMaskPolicyJson) {
        this.columnMaskPolicyJson = columnMaskPolicyJson;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getDeletedFlag() {
        return deletedFlag;
    }

    public void setDeletedFlag(Long deletedFlag) {
        this.deletedFlag = deletedFlag;
    }
}
