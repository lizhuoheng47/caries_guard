package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("med_case_tooth_record")
public class ReportToothRecordDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long caseId;
    private Long sourceImageId;
    private String toothCode;
    private String toothSurfaceCode;
    private String issueTypeCode;
    private String severityCode;
    private String findingDesc;
    private String suggestion;
    private Integer sortOrder;
    private String status;
    private Long deletedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getSourceImageId() { return sourceImageId; }
    public void setSourceImageId(Long sourceImageId) { this.sourceImageId = sourceImageId; }
    public String getToothCode() { return toothCode; }
    public void setToothCode(String toothCode) { this.toothCode = toothCode; }
    public String getToothSurfaceCode() { return toothSurfaceCode; }
    public void setToothSurfaceCode(String toothSurfaceCode) { this.toothSurfaceCode = toothSurfaceCode; }
    public String getIssueTypeCode() { return issueTypeCode; }
    public void setIssueTypeCode(String issueTypeCode) { this.issueTypeCode = issueTypeCode; }
    public String getSeverityCode() { return severityCode; }
    public void setSeverityCode(String severityCode) { this.severityCode = severityCode; }
    public String getFindingDesc() { return findingDesc; }
    public void setFindingDesc(String findingDesc) { this.findingDesc = findingDesc; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}
