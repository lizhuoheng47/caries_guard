package com.cariesguard.analysis.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("med_image_file")
public class AnalysisImageFileDO {

    private Long id;
    private Long caseId;
    private Long attachmentId;
    private String imageTypeCode;
    private String qualityStatusCode;
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}
