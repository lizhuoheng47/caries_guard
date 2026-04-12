package com.cariesguard.analysis.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ana_result_summary")
public class AnaResultSummaryDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long taskId;
    private Long caseId;
    private String rawResultJson;
    private String overallHighestSeverity;
    private java.math.BigDecimal uncertaintyScore;
    private String reviewSuggestedFlag;
    private Long orgId;
    private String status;
    private Long deletedFlag;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getRawResultJson() { return rawResultJson; }
    public void setRawResultJson(String rawResultJson) { this.rawResultJson = rawResultJson; }
    public String getOverallHighestSeverity() { return overallHighestSeverity; }
    public void setOverallHighestSeverity(String overallHighestSeverity) { this.overallHighestSeverity = overallHighestSeverity; }
    public java.math.BigDecimal getUncertaintyScore() { return uncertaintyScore; }
    public void setUncertaintyScore(java.math.BigDecimal uncertaintyScore) { this.uncertaintyScore = uncertaintyScore; }
    public String getReviewSuggestedFlag() { return reviewSuggestedFlag; }
    public void setReviewSuggestedFlag(String reviewSuggestedFlag) { this.reviewSuggestedFlag = reviewSuggestedFlag; }
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
