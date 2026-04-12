package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("med_case")
public class ReportCaseDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String caseNo;
    private Long patientId;
    private String caseStatusCode;
    private Long orgId;
    private String status;
    private Long deletedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseNo() { return caseNo; }
    public void setCaseNo(String caseNo) { this.caseNo = caseNo; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getCaseStatusCode() { return caseStatusCode; }
    public void setCaseStatusCode(String caseStatusCode) { this.caseStatusCode = caseStatusCode; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}

