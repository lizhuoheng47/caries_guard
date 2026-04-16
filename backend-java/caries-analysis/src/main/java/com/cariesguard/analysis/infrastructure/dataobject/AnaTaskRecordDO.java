package com.cariesguard.analysis.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ana_task_record")
public class AnaTaskRecordDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String taskNo;
    private Long caseId;
    private Long patientId;
    private String requestBatchNo;
    private String modelVersion;
    private String taskTypeCode;
    private String taskStatusCode;
    private String requestPayloadJson;
    private String callbackPayloadJson;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorCode;
    private String errorMessage;
    private String traceId;
    private Integer inferenceMillis;
    private Long orgId;
    private Long retryFromTaskId;
    private String status;
    private Long deletedFlag;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskNo() { return taskNo; }
    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getRequestBatchNo() { return requestBatchNo; }
    public void setRequestBatchNo(String requestBatchNo) { this.requestBatchNo = requestBatchNo; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getTaskTypeCode() { return taskTypeCode; }
    public void setTaskTypeCode(String taskTypeCode) { this.taskTypeCode = taskTypeCode; }
    public String getTaskStatusCode() { return taskStatusCode; }
    public void setTaskStatusCode(String taskStatusCode) { this.taskStatusCode = taskStatusCode; }
    public String getRequestPayloadJson() { return requestPayloadJson; }
    public void setRequestPayloadJson(String requestPayloadJson) { this.requestPayloadJson = requestPayloadJson; }
    public String getCallbackPayloadJson() { return callbackPayloadJson; }
    public void setCallbackPayloadJson(String callbackPayloadJson) { this.callbackPayloadJson = callbackPayloadJson; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Integer getInferenceMillis() { return inferenceMillis; }
    public void setInferenceMillis(Integer inferenceMillis) { this.inferenceMillis = inferenceMillis; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public Long getRetryFromTaskId() { return retryFromTaskId; }
    public void setRetryFromTaskId(Long retryFromTaskId) { this.retryFromTaskId = retryFromTaskId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
