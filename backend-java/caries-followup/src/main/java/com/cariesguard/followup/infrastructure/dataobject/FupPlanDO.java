package com.cariesguard.followup.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("fup_plan")
public class FupPlanDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String planNo;
    private Long caseId;
    private Long patientId;
    private String planTypeCode;
    private String planStatusCode;
    private LocalDate nextFollowupDate;
    private Integer intervalDays;
    private Long ownerUserId;
    private Long orgId;
    private String status;
    private Long deletedFlag;
    private String remark;
    private String triggerSourceCode;
    private Long triggerRefId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanNo() { return planNo; }
    public void setPlanNo(String planNo) { this.planNo = planNo; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPlanTypeCode() { return planTypeCode; }
    public void setPlanTypeCode(String planTypeCode) { this.planTypeCode = planTypeCode; }
    public String getPlanStatusCode() { return planStatusCode; }
    public void setPlanStatusCode(String planStatusCode) { this.planStatusCode = planStatusCode; }
    public LocalDate getNextFollowupDate() { return nextFollowupDate; }
    public void setNextFollowupDate(LocalDate nextFollowupDate) { this.nextFollowupDate = nextFollowupDate; }
    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getTriggerSourceCode() { return triggerSourceCode; }
    public void setTriggerSourceCode(String triggerSourceCode) { this.triggerSourceCode = triggerSourceCode; }
    public Long getTriggerRefId() { return triggerRefId; }
    public void setTriggerRefId(Long triggerRefId) { this.triggerRefId = triggerRefId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
