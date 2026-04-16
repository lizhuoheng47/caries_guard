package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("med_attachment")
public class ReportAttachmentDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String bizModuleCode;
    private Long bizId;
    private String fileCategoryCode;
    private String assetTypeCode;
    private Long sourceAttachmentId;
    private String fileName;
    private String originalName;
    private String bucketName;
    private String objectKey;
    private String contentType;
    private String fileExt;
    private Long fileSizeBytes;
    private String md5;
    private String storageProviderCode;
    private String visibilityCode;
    private String retentionPolicyCode;
    private LocalDateTime expiredAt;
    private String integrityStatusCode;
    private String metadataJson;
    private Long uploadUserId;
    private LocalDateTime uploadTime;
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
    public String getBizModuleCode() { return bizModuleCode; }
    public void setBizModuleCode(String bizModuleCode) { this.bizModuleCode = bizModuleCode; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public String getFileCategoryCode() { return fileCategoryCode; }
    public void setFileCategoryCode(String fileCategoryCode) { this.fileCategoryCode = fileCategoryCode; }
    public String getAssetTypeCode() { return assetTypeCode; }
    public void setAssetTypeCode(String assetTypeCode) { this.assetTypeCode = assetTypeCode; }
    public Long getSourceAttachmentId() { return sourceAttachmentId; }
    public void setSourceAttachmentId(Long sourceAttachmentId) { this.sourceAttachmentId = sourceAttachmentId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getFileExt() { return fileExt; }
    public void setFileExt(String fileExt) { this.fileExt = fileExt; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getMd5() { return md5; }
    public void setMd5(String md5) { this.md5 = md5; }
    public String getStorageProviderCode() { return storageProviderCode; }
    public void setStorageProviderCode(String storageProviderCode) { this.storageProviderCode = storageProviderCode; }
    public String getVisibilityCode() { return visibilityCode; }
    public void setVisibilityCode(String visibilityCode) { this.visibilityCode = visibilityCode; }
    public String getRetentionPolicyCode() { return retentionPolicyCode; }
    public void setRetentionPolicyCode(String retentionPolicyCode) { this.retentionPolicyCode = retentionPolicyCode; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
    public String getIntegrityStatusCode() { return integrityStatusCode; }
    public void setIntegrityStatusCode(String integrityStatusCode) { this.integrityStatusCode = integrityStatusCode; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Long getUploadUserId() { return uploadUserId; }
    public void setUploadUserId(Long uploadUserId) { this.uploadUserId = uploadUserId; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
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
