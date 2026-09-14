package com.cariesguard.analysis.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ana_review_draft")
public class AnaReviewDraftDO {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long taskId;
    private Long caseId;
    private Long doctorUserId;
    private String revisedGrade;
    private String revisedDetectionsJson;
    private String reasonTagsJson;
    private String note;
    private String draftStatusCode;
    private Long submittedFeedbackId;
    private Integer versionNo;
    private Long orgId;
    private String status;
    private Long deletedFlag;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getDoctorUserId() { return doctorUserId; }
    public void setDoctorUserId(Long doctorUserId) { this.doctorUserId = doctorUserId; }
    public String getRevisedGrade() { return revisedGrade; }
    public void setRevisedGrade(String revisedGrade) { this.revisedGrade = revisedGrade; }
    public String getRevisedDetectionsJson() { return revisedDetectionsJson; }
    public void setRevisedDetectionsJson(String revisedDetectionsJson) { this.revisedDetectionsJson = revisedDetectionsJson; }
    public String getReasonTagsJson() { return reasonTagsJson; }
    public void setReasonTagsJson(String reasonTagsJson) { this.reasonTagsJson = reasonTagsJson; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getDraftStatusCode() { return draftStatusCode; }
    public void setDraftStatusCode(String draftStatusCode) { this.draftStatusCode = draftStatusCode; }
    public Long getSubmittedFeedbackId() { return submittedFeedbackId; }
    public void setSubmittedFeedbackId(Long submittedFeedbackId) { this.submittedFeedbackId = submittedFeedbackId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
