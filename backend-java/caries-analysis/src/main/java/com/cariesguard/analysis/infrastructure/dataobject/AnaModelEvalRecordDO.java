package com.cariesguard.analysis.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ana_model_eval_record")
public class AnaModelEvalRecordDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long modelVersionId;
    private Long datasetSnapshotId;
    private String evalTypeCode;
    private String metricJson;
    private String errorCaseJson;
    private String evidenceAttachmentKey;
    private LocalDateTime evaluatedAt;
    private Long evaluatorUserId;
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
    public Long getModelVersionId() { return modelVersionId; }
    public void setModelVersionId(Long modelVersionId) { this.modelVersionId = modelVersionId; }
    public Long getDatasetSnapshotId() { return datasetSnapshotId; }
    public void setDatasetSnapshotId(Long datasetSnapshotId) { this.datasetSnapshotId = datasetSnapshotId; }
    public String getEvalTypeCode() { return evalTypeCode; }
    public void setEvalTypeCode(String evalTypeCode) { this.evalTypeCode = evalTypeCode; }
    public String getMetricJson() { return metricJson; }
    public void setMetricJson(String metricJson) { this.metricJson = metricJson; }
    public String getErrorCaseJson() { return errorCaseJson; }
    public void setErrorCaseJson(String errorCaseJson) { this.errorCaseJson = errorCaseJson; }
    public String getEvidenceAttachmentKey() { return evidenceAttachmentKey; }
    public void setEvidenceAttachmentKey(String evidenceAttachmentKey) { this.evidenceAttachmentKey = evidenceAttachmentKey; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public Long getEvaluatorUserId() { return evaluatorUserId; }
    public void setEvaluatorUserId(Long evaluatorUserId) { this.evaluatorUserId = evaluatorUserId; }
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
