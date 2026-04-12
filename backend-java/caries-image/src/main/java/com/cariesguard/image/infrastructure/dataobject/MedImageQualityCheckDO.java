package com.cariesguard.image.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("med_image_quality_check")
public class MedImageQualityCheckDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long imageId;
    private Long caseId;
    private Long patientId;
    private String checkTypeCode;
    private String checkResultCode;
    private Integer qualityScore;
    private Integer blurScore;
    private Integer exposureScore;
    private Integer integrityScore;
    private Integer occlusionScore;
    private String issueCodesJson;
    private String suggestionText;
    private String currentFlag;
    private Long checkedBy;
    private LocalDateTime checkedAt;
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
    public Long getImageId() { return imageId; }
    public void setImageId(Long imageId) { this.imageId = imageId; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getCheckTypeCode() { return checkTypeCode; }
    public void setCheckTypeCode(String checkTypeCode) { this.checkTypeCode = checkTypeCode; }
    public String getCheckResultCode() { return checkResultCode; }
    public void setCheckResultCode(String checkResultCode) { this.checkResultCode = checkResultCode; }
    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }
    public Integer getBlurScore() { return blurScore; }
    public void setBlurScore(Integer blurScore) { this.blurScore = blurScore; }
    public Integer getExposureScore() { return exposureScore; }
    public void setExposureScore(Integer exposureScore) { this.exposureScore = exposureScore; }
    public Integer getIntegrityScore() { return integrityScore; }
    public void setIntegrityScore(Integer integrityScore) { this.integrityScore = integrityScore; }
    public Integer getOcclusionScore() { return occlusionScore; }
    public void setOcclusionScore(Integer occlusionScore) { this.occlusionScore = occlusionScore; }
    public String getIssueCodesJson() { return issueCodesJson; }
    public void setIssueCodesJson(String issueCodesJson) { this.issueCodesJson = issueCodesJson; }
    public String getSuggestionText() { return suggestionText; }
    public void setSuggestionText(String suggestionText) { this.suggestionText = suggestionText; }
    public String getCurrentFlag() { return currentFlag; }
    public void setCurrentFlag(String currentFlag) { this.currentFlag = currentFlag; }
    public Long getCheckedBy() { return checkedBy; }
    public void setCheckedBy(Long checkedBy) { this.checkedBy = checkedBy; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
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
