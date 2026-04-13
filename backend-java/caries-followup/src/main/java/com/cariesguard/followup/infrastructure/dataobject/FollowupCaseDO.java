package com.cariesguard.followup.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("med_case")
public class FollowupCaseDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long patientId;
    private String caseStatusCode;
    private Long orgId;
    private Long deletedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getCaseStatusCode() { return caseStatusCode; }
    public void setCaseStatusCode(String caseStatusCode) { this.caseStatusCode = caseStatusCode; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}
