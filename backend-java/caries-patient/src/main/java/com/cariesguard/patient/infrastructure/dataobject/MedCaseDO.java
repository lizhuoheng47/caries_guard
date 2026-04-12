package com.cariesguard.patient.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("med_case")
public class MedCaseDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String caseNo;
    private Long visitId;
    private Long patientId;
    private String caseTitle;
    private String caseTypeCode;
    private String caseStatusCode;
    private String priorityCode;
    private String chiefComplaint;
    private String clinicalNotes;
    private LocalDate onsetDate;
    private LocalDateTime firstDiagnosisAt;
    private Long attendingDoctorId;
    private Long screenerUserId;
    private String reportReadyFlag;
    private String followupRequiredFlag;
    private LocalDateTime closedAt;
    private Long orgId;
    private Integer versionNo;
    private String status;
    private Long deletedFlag;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCaseNo() { return caseNo; }
    public void setCaseNo(String caseNo) { this.caseNo = caseNo; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getCaseTitle() { return caseTitle; }
    public void setCaseTitle(String caseTitle) { this.caseTitle = caseTitle; }
    public String getCaseTypeCode() { return caseTypeCode; }
    public void setCaseTypeCode(String caseTypeCode) { this.caseTypeCode = caseTypeCode; }
    public String getCaseStatusCode() { return caseStatusCode; }
    public void setCaseStatusCode(String caseStatusCode) { this.caseStatusCode = caseStatusCode; }
    public String getPriorityCode() { return priorityCode; }
    public void setPriorityCode(String priorityCode) { this.priorityCode = priorityCode; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
    public String getClinicalNotes() { return clinicalNotes; }
    public void setClinicalNotes(String clinicalNotes) { this.clinicalNotes = clinicalNotes; }
    public LocalDate getOnsetDate() { return onsetDate; }
    public void setOnsetDate(LocalDate onsetDate) { this.onsetDate = onsetDate; }
    public LocalDateTime getFirstDiagnosisAt() { return firstDiagnosisAt; }
    public void setFirstDiagnosisAt(LocalDateTime firstDiagnosisAt) { this.firstDiagnosisAt = firstDiagnosisAt; }
    public Long getAttendingDoctorId() { return attendingDoctorId; }
    public void setAttendingDoctorId(Long attendingDoctorId) { this.attendingDoctorId = attendingDoctorId; }
    public Long getScreenerUserId() { return screenerUserId; }
    public void setScreenerUserId(Long screenerUserId) { this.screenerUserId = screenerUserId; }
    public String getReportReadyFlag() { return reportReadyFlag; }
    public void setReportReadyFlag(String reportReadyFlag) { this.reportReadyFlag = reportReadyFlag; }
    public String getFollowupRequiredFlag() { return followupRequiredFlag; }
    public void setFollowupRequiredFlag(String followupRequiredFlag) { this.followupRequiredFlag = followupRequiredFlag; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
