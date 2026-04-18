package com.cariesguard.analysis.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("trn_dataset_sample")
public class TrnDatasetSampleDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long snapshotId;
    private String sampleRefNo;
    private String patientUuid;
    private String imageRefNo;
    private String sourceTypeCode;
    private String splitTypeCode;
    private String labelVersion;
    private String labelJson;
    private Long orgId;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public String getSampleRefNo() { return sampleRefNo; }
    public void setSampleRefNo(String sampleRefNo) { this.sampleRefNo = sampleRefNo; }
    public String getPatientUuid() { return patientUuid; }
    public void setPatientUuid(String patientUuid) { this.patientUuid = patientUuid; }
    public String getImageRefNo() { return imageRefNo; }
    public void setImageRefNo(String imageRefNo) { this.imageRefNo = imageRefNo; }
    public String getSourceTypeCode() { return sourceTypeCode; }
    public void setSourceTypeCode(String sourceTypeCode) { this.sourceTypeCode = sourceTypeCode; }
    public String getSplitTypeCode() { return splitTypeCode; }
    public void setSplitTypeCode(String splitTypeCode) { this.splitTypeCode = splitTypeCode; }
    public String getLabelVersion() { return labelVersion; }
    public void setLabelVersion(String labelVersion) { this.labelVersion = labelVersion; }
    public String getLabelJson() { return labelJson; }
    public void setLabelJson(String labelJson) { this.labelJson = labelJson; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
