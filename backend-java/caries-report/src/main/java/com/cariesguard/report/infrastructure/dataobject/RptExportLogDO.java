package com.cariesguard.report.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("rpt_export_log")
public class RptExportLogDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long reportId;
    private Long attachmentId;
    private String exportTypeCode;
    private String exportChannelCode;
    private Long exportedBy;
    private LocalDateTime exportedAt;
    private Long orgId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Long getAttachmentId() { return attachmentId; }
    public void setAttachmentId(Long attachmentId) { this.attachmentId = attachmentId; }
    public String getExportTypeCode() { return exportTypeCode; }
    public void setExportTypeCode(String exportTypeCode) { this.exportTypeCode = exportTypeCode; }
    public String getExportChannelCode() { return exportChannelCode; }
    public void setExportChannelCode(String exportChannelCode) { this.exportChannelCode = exportChannelCode; }
    public Long getExportedBy() { return exportedBy; }
    public void setExportedBy(Long exportedBy) { this.exportedBy = exportedBy; }
    public LocalDateTime getExportedAt() { return exportedAt; }
    public void setExportedAt(LocalDateTime exportedAt) { this.exportedAt = exportedAt; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
}

