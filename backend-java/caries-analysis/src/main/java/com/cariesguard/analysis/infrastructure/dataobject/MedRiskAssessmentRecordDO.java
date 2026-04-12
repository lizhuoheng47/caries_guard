package com.cariesguard.analysis.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("med_risk_assessment_record")
public class MedRiskAssessmentRecordDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private Long patientId;
    private String overallRiskLevelCode;
    private String assessmentReportJson;
    private Integer recommendedCycleDays;
    private LocalDateTime assessedAt;
    private Long orgId;
    private String status;
    private Long deletedFlag;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getOverallRiskLevelCode() { return overallRiskLevelCode; }
    public void setOverallRiskLevelCode(String overallRiskLevelCode) { this.overallRiskLevelCode = overallRiskLevelCode; }
    public String getAssessmentReportJson() { return assessmentReportJson; }
    public void setAssessmentReportJson(String assessmentReportJson) { this.assessmentReportJson = assessmentReportJson; }
    public Integer getRecommendedCycleDays() { return recommendedCycleDays; }
    public void setRecommendedCycleDays(Integer recommendedCycleDays) { this.recommendedCycleDays = recommendedCycleDays; }
    public LocalDateTime getAssessedAt() { return assessedAt; }
    public void setAssessedAt(LocalDateTime assessedAt) { this.assessedAt = assessedAt; }
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
