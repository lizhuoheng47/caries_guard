package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ana_correction_feedback")
public class ReportCorrectionFeedbackDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private Long diagnosisId;
    private Long sourceImageId;
    private Long sourceAttachmentId;
    private String feedbackTypeCode;
    private String exportCandidateFlag;
    private String exportedSnapshotNo;
    private String correctedTruthJson;
    private String status;
    private Long deletedFlag;
    private LocalDateTime createdAt;

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
    public String getFeedbackTypeCode() { return feedbackTypeCode; }
    public void setFeedbackTypeCode(String feedbackTypeCode) { this.feedbackTypeCode = feedbackTypeCode; }
    public String getExportCandidateFlag() { return exportCandidateFlag; }
    public void setExportCandidateFlag(String exportCandidateFlag) { this.exportCandidateFlag = exportCandidateFlag; }
    public String getExportedSnapshotNo() { return exportedSnapshotNo; }
    public void setExportedSnapshotNo(String exportedSnapshotNo) { this.exportedSnapshotNo = exportedSnapshotNo; }
    public String getCorrectedTruthJson() { return correctedTruthJson; }
    public void setCorrectedTruthJson(String correctedTruthJson) { this.correctedTruthJson = correctedTruthJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
