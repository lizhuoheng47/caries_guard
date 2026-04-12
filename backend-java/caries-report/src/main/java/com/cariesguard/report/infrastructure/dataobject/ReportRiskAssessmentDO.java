package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("med_risk_assessment_record")
public class ReportRiskAssessmentDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private String overallRiskLevelCode;
    private String assessmentReportJson;
    private Integer recommendedCycleDays;
    private LocalDateTime assessedAt;
    private String status;
    private Long deletedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getOverallRiskLevelCode() { return overallRiskLevelCode; }
    public void setOverallRiskLevelCode(String overallRiskLevelCode) { this.overallRiskLevelCode = overallRiskLevelCode; }
    public String getAssessmentReportJson() { return assessmentReportJson; }
    public void setAssessmentReportJson(String assessmentReportJson) { this.assessmentReportJson = assessmentReportJson; }
    public Integer getRecommendedCycleDays() { return recommendedCycleDays; }
    public void setRecommendedCycleDays(Integer recommendedCycleDays) { this.recommendedCycleDays = recommendedCycleDays; }
    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}

