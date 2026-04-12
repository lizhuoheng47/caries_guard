package com.cariesguard.patient.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("med_case_tooth_record")
public class MedCaseToothRecordDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private Long sourceImageId;
    private String toothCode;
    private String toothSurfaceCode;
    private String issueTypeCode;
    private String severityCode;
    private String findingDesc;
    private String suggestion;
    private Integer sortOrder;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
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
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getSourceImageId() { return sourceImageId; }
    public void setSourceImageId(Long sourceImageId) { this.sourceImageId = sourceImageId; }
    public String getToothCode() { return toothCode; }
    public void setToothCode(String toothCode) { this.toothCode = toothCode; }
    public String getToothSurfaceCode() { return toothSurfaceCode; }
    public void setToothSurfaceCode(String toothSurfaceCode) { this.toothSurfaceCode = toothSurfaceCode; }
    public String getIssueTypeCode() { return issueTypeCode; }
    public void setIssueTypeCode(String issueTypeCode) { this.issueTypeCode = issueTypeCode; }
    public String getSeverityCode() { return severityCode; }
    public void setSeverityCode(String severityCode) { this.severityCode = severityCode; }
    public String getFindingDesc() { return findingDesc; }
    public void setFindingDesc(String findingDesc) { this.findingDesc = findingDesc; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
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
