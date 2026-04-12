package com.cariesguard.patient.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("med_case_diagnosis")
public class MedCaseDiagnosisDO {

    private Long id;
    private Long caseId;
    private String diagnosisName;
    private String severityCode;
    private String isFinal;
    private String status;
    private Long deletedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getDiagnosisName() { return diagnosisName; }
    public void setDiagnosisName(String diagnosisName) { this.diagnosisName = diagnosisName; }
    public String getSeverityCode() { return severityCode; }
    public void setSeverityCode(String severityCode) { this.severityCode = severityCode; }
    public String getIsFinal() { return isFinal; }
    public void setIsFinal(String isFinal) { this.isFinal = isFinal; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}
