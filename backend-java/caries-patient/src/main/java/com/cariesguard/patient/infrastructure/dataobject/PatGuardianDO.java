package com.cariesguard.patient.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("pat_guardian")
public class PatGuardianDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long patientId;
    private String guardianNameEnc;
    private String guardianNameHash;
    private String guardianNameMasked;
    private String relationCode;
    private String phoneEnc;
    private String phoneHash;
    private String phoneMasked;
    private String certificateTypeCode;
    private String certificateNoEnc;
    private String certificateNoHash;
    private String certificateNoMasked;
    private String isPrimary;
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
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getGuardianNameEnc() { return guardianNameEnc; }
    public void setGuardianNameEnc(String guardianNameEnc) { this.guardianNameEnc = guardianNameEnc; }
    public String getGuardianNameHash() { return guardianNameHash; }
    public void setGuardianNameHash(String guardianNameHash) { this.guardianNameHash = guardianNameHash; }
    public String getGuardianNameMasked() { return guardianNameMasked; }
    public void setGuardianNameMasked(String guardianNameMasked) { this.guardianNameMasked = guardianNameMasked; }
    public String getRelationCode() { return relationCode; }
    public void setRelationCode(String relationCode) { this.relationCode = relationCode; }
    public String getPhoneEnc() { return phoneEnc; }
    public void setPhoneEnc(String phoneEnc) { this.phoneEnc = phoneEnc; }
    public String getPhoneHash() { return phoneHash; }
    public void setPhoneHash(String phoneHash) { this.phoneHash = phoneHash; }
    public String getPhoneMasked() { return phoneMasked; }
    public void setPhoneMasked(String phoneMasked) { this.phoneMasked = phoneMasked; }
    public String getCertificateTypeCode() { return certificateTypeCode; }
    public void setCertificateTypeCode(String certificateTypeCode) { this.certificateTypeCode = certificateTypeCode; }
    public String getCertificateNoEnc() { return certificateNoEnc; }
    public void setCertificateNoEnc(String certificateNoEnc) { this.certificateNoEnc = certificateNoEnc; }
    public String getCertificateNoHash() { return certificateNoHash; }
    public void setCertificateNoHash(String certificateNoHash) { this.certificateNoHash = certificateNoHash; }
    public String getCertificateNoMasked() { return certificateNoMasked; }
    public void setCertificateNoMasked(String certificateNoMasked) { this.certificateNoMasked = certificateNoMasked; }
    public String getIsPrimary() { return isPrimary; }
    public void setIsPrimary(String isPrimary) { this.isPrimary = isPrimary; }
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
