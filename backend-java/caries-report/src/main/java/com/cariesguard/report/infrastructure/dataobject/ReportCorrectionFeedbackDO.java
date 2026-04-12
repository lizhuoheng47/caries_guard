package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("ana_correction_feedback")
public class ReportCorrectionFeedbackDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private String feedbackTypeCode;
    private String correctedTruthJson;
    private String status;
    private Long deletedFlag;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public String getFeedbackTypeCode() { return feedbackTypeCode; }
    public void setFeedbackTypeCode(String feedbackTypeCode) { this.feedbackTypeCode = feedbackTypeCode; }
    public String getCorrectedTruthJson() { return correctedTruthJson; }
    public void setCorrectedTruthJson(String correctedTruthJson) { this.correctedTruthJson = correctedTruthJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

