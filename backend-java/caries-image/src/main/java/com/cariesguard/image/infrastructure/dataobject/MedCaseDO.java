package com.cariesguard.image.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("med_case")
public class MedCaseDO {

    private Long id;
    private String caseNo;
    private Long visitId;
    private Long patientId;
    private Long orgId;
    private String caseStatusCode;
    private String status;
    private Long deletedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseNo() { return caseNo; }
    public void setCaseNo(String caseNo) { this.caseNo = caseNo; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getCaseStatusCode() { return caseStatusCode; }
    public void setCaseStatusCode(String caseStatusCode) { this.caseStatusCode = caseStatusCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}
