package com.cariesguard.patient.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("ana_result_summary")
public class AnaResultSummaryDO {

    private Long id;
    private Long caseId;
    private String rawResultJson;
    private String overallHighestSeverity;
    private BigDecimal uncertaintyScore;
    private String reviewSuggestedFlag;
    private Integer lesionCount;
    private Integer abnormalToothCount;
    private Integer summaryVersionNo;
    private String status;
    private Long deletedFlag;

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}
