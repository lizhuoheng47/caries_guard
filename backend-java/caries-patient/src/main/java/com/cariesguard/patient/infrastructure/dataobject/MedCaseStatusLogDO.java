package com.cariesguard.patient.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("med_case_status_log")
public class MedCaseStatusLogDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private String fromStatusCode;
    private String toStatusCode;
    private Long changedBy;
    private String changeReasonCode;
    private String changeReason;
    private LocalDateTime changedAt;
    private Long orgId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getFromStatusCode() { return fromStatusCode; }
    public void setFromStatusCode(String fromStatusCode) { this.fromStatusCode = fromStatusCode; }
    public String getToStatusCode() { return toStatusCode; }
    public void setToStatusCode(String toStatusCode) { this.toStatusCode = toStatusCode; }
    public Long getChangedBy() { return changedBy; }
    public void setChangedBy(Long changedBy) { this.changedBy = changedBy; }
    public String getChangeReasonCode() { return changeReasonCode; }
    public void setChangeReasonCode(String changeReasonCode) { this.changeReasonCode = changeReasonCode; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
}
