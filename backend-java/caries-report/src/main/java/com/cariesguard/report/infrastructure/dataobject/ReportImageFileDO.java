package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("med_image_file")
public class ReportImageFileDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private Long attachmentId;
    private String imageTypeCode;
    private String qualityStatusCode;
    private String sourceDeviceCode;
    private String captureBatchNo;
    private String isPrimary;
    private String status;
    private Long deletedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getImageTypeCode() { return imageTypeCode; }
    public void setImageTypeCode(String imageTypeCode) { this.imageTypeCode = imageTypeCode; }
    public String getQualityStatusCode() { return qualityStatusCode; }
    public void setQualityStatusCode(String qualityStatusCode) { this.qualityStatusCode = qualityStatusCode; }
    public String getSourceDeviceCode() { return sourceDeviceCode; }
    public void setSourceDeviceCode(String sourceDeviceCode) { this.sourceDeviceCode = sourceDeviceCode; }
    public String getCaptureBatchNo() { return captureBatchNo; }
    public void setCaptureBatchNo(String captureBatchNo) { this.captureBatchNo = captureBatchNo; }
    public String getIsPrimary() { return isPrimary; }
    public void setIsPrimary(String isPrimary) { this.isPrimary = isPrimary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}
