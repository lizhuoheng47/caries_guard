package com.cariesguard.analysis.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ana_correction_feedback")
public class AnaCorrectionFeedbackDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private Long diagnosisId;
    private Long sourceImageId;
    private Long sourceAttachmentId;
    private Long doctorUserId;
    private String originalInferenceJson;
    private String correctedTruthJson;
    private String feedbackTypeCode;
    private String exportCandidateFlag;
    private String exportedSnapshotNo;
    private String trainingCandidateFlag;
    private String desensitizedExportFlag;
    private String reviewStatusCode;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private Long orgId;
    private String status;
    private Long deletedFlag;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getDiagnosisId() { return diagnosisId; }
    public void setDiagnosisId(Long diagnosisId) { this.diagnosisId = diagnosisId; }
    public Long getSourceImageId() { return sourceImageId; }
    public void setSourceImageId(Long sourceImageId) { this.sourceImageId = sourceImageId; }
    public Long getSourceAttachmentId() { return sourceAttachmentId; }
    public void setSourceAttachmentId(Long sourceAttachmentId) { this.sourceAttachmentId = sourceAttachmentId; }
    public Long getDoctorUserId() { return doctorUserId; }
    public void setDoctorUserId(Long doctorUserId) { this.doctorUserId = doctorUserId; }
    public String getOriginalInferenceJson() { return originalInferenceJson; }
    public void setOriginalInferenceJson(String originalInferenceJson) { this.originalInferenceJson = originalInferenceJson; }
    public String getCorrectedTruthJson() { return correctedTruthJson; }
    public void setCorrectedTruthJson(String correctedTruthJson) { this.correctedTruthJson = correctedTruthJson; }
    public String getFeedbackTypeCode() { return feedbackTypeCode; }
    public void setFeedbackTypeCode(String feedbackTypeCode) { this.feedbackTypeCode = feedbackTypeCode; }
    public String getExportCandidateFlag() { return exportCandidateFlag; }
    public void setExportCandidateFlag(String exportCandidateFlag) { this.exportCandidateFlag = exportCandidateFlag; }
    public String getExportedSnapshotNo() { return exportedSnapshotNo; }
    public void setExportedSnapshotNo(String exportedSnapshotNo) { this.exportedSnapshotNo = exportedSnapshotNo; }
    public String getTrainingCandidateFlag() { return trainingCandidateFlag; }
    public void setTrainingCandidateFlag(String trainingCandidateFlag) { this.trainingCandidateFlag = trainingCandidateFlag; }
    public String getDesensitizedExportFlag() { return desensitizedExportFlag; }
    public void setDesensitizedExportFlag(String desensitizedExportFlag) { this.desensitizedExportFlag = desensitizedExportFlag; }
    public String getReviewStatusCode() { return reviewStatusCode; }
    public void setReviewStatusCode(String reviewStatusCode) { this.reviewStatusCode = reviewStatusCode; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
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
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
