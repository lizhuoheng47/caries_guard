package com.cariesguard.system.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_dept")
public class SysDeptDO {

    private Long id;
    private Long orgId;
    private String status;
    private Long deletedFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
