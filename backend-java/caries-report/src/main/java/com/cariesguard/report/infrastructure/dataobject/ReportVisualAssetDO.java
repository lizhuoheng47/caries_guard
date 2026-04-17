package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ana_visual_asset")
public class ReportVisualAssetDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long taskId;
    private Long caseId;
    private String modelVersion;
    private String assetTypeCode;
    private Long attachmentId;
    private Long relatedImageId;
    private Long sourceAttachmentId;
    private String toothCode;
    private Integer sortOrder;
    private String status;
    private Long deletedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getAssetTypeCode() { return assetTypeCode; }
    public void setAssetTypeCode(String assetTypeCode) { this.assetTypeCode = assetTypeCode; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public Long getRelatedImageId() { return relatedImageId; }
    public void setRelatedImageId(Long relatedImageId) { this.relatedImageId = relatedImageId; }
    public Long getSourceAttachmentId() { return sourceAttachmentId; }
    public void setSourceAttachmentId(Long sourceAttachmentId) { this.sourceAttachmentId = sourceAttachmentId; }
    public String getToothCode() { return toothCode; }
    public void setToothCode(String toothCode) { this.toothCode = toothCode; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}
