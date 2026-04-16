package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ana_result_summary")
public class ReportResultSummaryDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private String rawResultJson;
    private String overallHighestSeverity;
    private BigDecimal uncertaintyScore;
    private String reviewSuggestedFlag;
    private Integer lesionCount;
    private Integer abnormalToothCount;
    private Integer summaryVersionNo;
    private Long orgId;
    private String status;
    private Long deletedFlag;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getRawResultJson() { return rawResultJson; }
    public void setRawResultJson(String rawResultJson) { this.rawResultJson = rawResultJson; }
    public String getOverallHighestSeverity() { return overallHighestSeverity; }
    public void setOverallHighestSeverity(String overallHighestSeverity) { this.overallHighestSeverity = overallHighestSeverity; }
    public BigDecimal getUncertaintyScore() { return uncertaintyScore; }
    public void setUncertaintyScore(BigDecimal uncertaintyScore) { this.uncertaintyScore = uncertaintyScore; }
    public String getReviewSuggestedFlag() { return reviewSuggestedFlag; }
    public void setReviewSuggestedFlag(String reviewSuggestedFlag) { this.reviewSuggestedFlag = reviewSuggestedFlag; }
    public Integer getLesionCount() { return lesionCount; }
    public void setLesionCount(Integer lesionCount) { this.lesionCount = lesionCount; }
    public Integer getAbnormalToothCount() { return abnormalToothCount; }
    public void setAbnormalToothCount(Integer abnormalToothCount) { this.abnormalToothCount = abnormalToothCount; }
    public Integer getSummaryVersionNo() { return summaryVersionNo; }
    public void setSummaryVersionNo(Integer summaryVersionNo) { this.summaryVersionNo = summaryVersionNo; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
