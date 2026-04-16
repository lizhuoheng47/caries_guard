package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("rpt_record")
public class RptRecordDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String reportNo;
    private Long caseId;
    private Long patientId;
    private Long attachmentId;
    private Long sourceSummaryId;
    private Long sourceRiskAssessmentId;
    private Long sourceCorrectionId;
    private String reportTypeCode;
    private String reportStatusCode;
    private Integer versionNo;
    private String summaryText;
    private LocalDateTime generatedAt;
    private LocalDateTime signedAt;
    private Long orgId;
    private String status;
    private Long deletedFlag;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = reportNo; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public Long getSourceSummaryId() { return sourceSummaryId; }
    public void setSourceSummaryId(Long sourceSummaryId) { this.sourceSummaryId = sourceSummaryId; }
    public Long getSourceRiskAssessmentId() { return sourceRiskAssessmentId; }
    public void setSourceRiskAssessmentId(Long sourceRiskAssessmentId) { this.sourceRiskAssessmentId = sourceRiskAssessmentId; }
    public Long getSourceCorrectionId() { return sourceCorrectionId; }
    public void setSourceCorrectionId(Long sourceCorrectionId) { this.sourceCorrectionId = sourceCorrectionId; }
    public String getReportTypeCode() { return reportTypeCode; }
    public void setReportTypeCode(String reportTypeCode) { this.reportTypeCode = reportTypeCode; }
    public String getReportStatusCode() { return reportStatusCode; }
    public void setReportStatusCode(String reportStatusCode) { this.reportStatusCode = reportStatusCode; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void setSignedAt(LocalDateTime signedAt) { this.signedAt = signedAt; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
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
