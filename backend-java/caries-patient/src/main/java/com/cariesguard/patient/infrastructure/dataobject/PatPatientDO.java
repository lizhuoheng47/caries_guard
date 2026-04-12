package com.cariesguard.patient.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("pat_patient")
public class PatPatientDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private String patientNo;
    private String patientNameEnc;
    private String patientNameHash;
    private String patientNameMasked;
    private String genderCode;
    private String birthDateEnc;
    private String birthDateHash;
    private String birthDateMasked;
    private Integer age;
    private String phoneEnc;
    private String phoneHash;
    private String phoneMasked;
    private String idCardEnc;
    private String idCardHash;
    private String idCardMasked;
    private String sourceCode;
    private LocalDate firstVisitDate;
    private String privacyLevelCode;
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
    public String getPatientNo() { return patientNo; }
    public void setPatientNo(String patientNo) { this.patientNo = patientNo; }
    public String getPatientNameEnc() { return patientNameEnc; }
    public void setPatientNameEnc(String patientNameEnc) { this.patientNameEnc = patientNameEnc; }
    public String getPatientNameHash() { return patientNameHash; }
    public void setPatientNameHash(String patientNameHash) { this.patientNameHash = patientNameHash; }
    public String getPatientNameMasked() { return patientNameMasked; }
    public void setPatientNameMasked(String patientNameMasked) { this.patientNameMasked = patientNameMasked; }
    public String getGenderCode() { return genderCode; }
    public void setGenderCode(String genderCode) { this.genderCode = genderCode; }
    public String getBirthDateEnc() { return birthDateEnc; }
    public void setBirthDateEnc(String birthDateEnc) { this.birthDateEnc = birthDateEnc; }
    public String getBirthDateHash() { return birthDateHash; }
    public void setBirthDateHash(String birthDateHash) { this.birthDateHash = birthDateHash; }
    public String getBirthDateMasked() { return birthDateMasked; }
    public void setBirthDateMasked(String birthDateMasked) { this.birthDateMasked = birthDateMasked; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getPhoneEnc() { return phoneEnc; }
    public void setPhoneEnc(String phoneEnc) { this.phoneEnc = phoneEnc; }
    public String getPhoneHash() { return phoneHash; }
    public void setPhoneHash(String phoneHash) { this.phoneHash = phoneHash; }
    public String getPhoneMasked() { return phoneMasked; }
    public void setPhoneMasked(String phoneMasked) { this.phoneMasked = phoneMasked; }
    public String getIdCardEnc() { return idCardEnc; }
    public void setIdCardEnc(String idCardEnc) { this.idCardEnc = idCardEnc; }
    public String getIdCardHash() { return idCardHash; }
    public void setIdCardHash(String idCardHash) { this.idCardHash = idCardHash; }
    public String getIdCardMasked() { return idCardMasked; }
    public void setIdCardMasked(String idCardMasked) { this.idCardMasked = idCardMasked; }
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }
    public LocalDate getFirstVisitDate() { return firstVisitDate; }
    public void setFirstVisitDate(LocalDate firstVisitDate) { this.firstVisitDate = firstVisitDate; }
    public String getPrivacyLevelCode() { return privacyLevelCode; }
    public void setPrivacyLevelCode(String privacyLevelCode) { this.privacyLevelCode = privacyLevelCode; }
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
