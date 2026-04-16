package com.cariesguard.image.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("med_image_file")
public class MedImageFileDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private Long visitId;
    private Long patientId;
    private Long attachmentId;
    private String imageTypeCode;
    private String imageSourceCode;
    private LocalDateTime shootingTime;
    private String bodyPositionCode;
    private Integer imageIndexNo;
    private String qualityStatusCode;
    private String sourceDeviceCode;
    private String captureBatchNo;
    private String isPrimary;
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
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getImageTypeCode() { return imageTypeCode; }
    public void setImageTypeCode(String imageTypeCode) { this.imageTypeCode = imageTypeCode; }
    public String getImageSourceCode() { return imageSourceCode; }
    public void setImageSourceCode(String imageSourceCode) { this.imageSourceCode = imageSourceCode; }
    public LocalDateTime getShootingTime() { return shootingTime; }
    public void setShootingTime(LocalDateTime shootingTime) { this.shootingTime = shootingTime; }
    public String getBodyPositionCode() { return bodyPositionCode; }
    public void setBodyPositionCode(String bodyPositionCode) { this.bodyPositionCode = bodyPositionCode; }
    public Integer getImageIndexNo() { return imageIndexNo; }
    public void setImageIndexNo(Integer imageIndexNo) { this.imageIndexNo = imageIndexNo; }
    public String getQualityStatusCode() { return qualityStatusCode; }
    public void setQualityStatusCode(String qualityStatusCode) { this.qualityStatusCode = qualityStatusCode; }
    public String getSourceDeviceCode() { return sourceDeviceCode; }
    public void setSourceDeviceCode(String sourceDeviceCode) { this.sourceDeviceCode = sourceDeviceCode; }
    public String getCaptureBatchNo() { return captureBatchNo; }
    public void setCaptureBatchNo(String captureBatchNo) { this.captureBatchNo = captureBatchNo; }
    public String getIsPrimary() { return isPrimary; }
    public void setIsPrimary(String isPrimary) { this.isPrimary = isPrimary; }
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
