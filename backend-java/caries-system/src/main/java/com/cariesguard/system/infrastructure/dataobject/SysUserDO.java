package com.cariesguard.system.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("sys_user")
public class SysUserDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long deptId;
    private String userNo;
    private String username;
    private String passwordHash;
    private String realNameEnc;
    private String realNameHash;
    private String realNameMasked;
    private String nickName;
    private String userTypeCode;
    private String genderCode;
    private String phoneEnc;
    private String phoneHash;
    private String phoneMasked;
    private String emailEnc;
    private String emailHash;
    private String emailMasked;
    private String avatarUrl;
    private String certificateTypeCode;
    private String certificateNoEnc;
    private String certificateNoHash;
    private String certificateNoMasked;
    private LocalDateTime lastLoginAt;
    private LocalDateTime pwdUpdatedAt;
    private Long orgId;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private Long deletedFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRealNameEnc() {
        return realNameEnc;
    }

    public void setRealNameEnc(String realNameEnc) {
        this.realNameEnc = realNameEnc;
    }

    public String getRealNameHash() {
        return realNameHash;
    }

    public void setRealNameHash(String realNameHash) {
        this.realNameHash = realNameHash;
    }

    public String getRealNameMasked() {
        return realNameMasked;
    }

    public void setRealNameMasked(String realNameMasked) {
        this.realNameMasked = realNameMasked;
    }

    public String getUserTypeCode() {
        return userTypeCode;
    }

    public void setUserTypeCode(String userTypeCode) {
        this.userTypeCode = userTypeCode;
    }

    public String getGenderCode() {
        return genderCode;
    }

    public void setGenderCode(String genderCode) {
        this.genderCode = genderCode;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getPhoneEnc() {
        return phoneEnc;
    }

    public void setPhoneEnc(String phoneEnc) {
        this.phoneEnc = phoneEnc;
    }

    public String getPhoneHash() {
        return phoneHash;
    }

    public void setPhoneHash(String phoneHash) {
        this.phoneHash = phoneHash;
    }

    public String getPhoneMasked() {
        return phoneMasked;
    }

    public void setPhoneMasked(String phoneMasked) {
        this.phoneMasked = phoneMasked;
    }

    public String getEmailEnc() {
        return emailEnc;
    }

    public void setEmailEnc(String emailEnc) {
        this.emailEnc = emailEnc;
    }

    public String getEmailHash() {
        return emailHash;
    }

    public void setEmailHash(String emailHash) {
        this.emailHash = emailHash;
    }

    public String getEmailMasked() {
        return emailMasked;
    }

    public void setEmailMasked(String emailMasked) {
        this.emailMasked = emailMasked;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCertificateTypeCode() {
        return certificateTypeCode;
    }

    public void setCertificateTypeCode(String certificateTypeCode) {
        this.certificateTypeCode = certificateTypeCode;
    }

    public String getCertificateNoEnc() {
        return certificateNoEnc;
    }

    public void setCertificateNoEnc(String certificateNoEnc) {
        this.certificateNoEnc = certificateNoEnc;
    }

    public String getCertificateNoHash() {
        return certificateNoHash;
    }

    public void setCertificateNoHash(String certificateNoHash) {
        this.certificateNoHash = certificateNoHash;
    }

    public String getCertificateNoMasked() {
        return certificateNoMasked;
    }

    public void setCertificateNoMasked(String certificateNoMasked) {
        this.certificateNoMasked = certificateNoMasked;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getPwdUpdatedAt() {
        return pwdUpdatedAt;
    }

    public void setPwdUpdatedAt(LocalDateTime pwdUpdatedAt) {
        this.pwdUpdatedAt = pwdUpdatedAt;
    }

    public Long getDeletedFlag() {
        return deletedFlag;
    }

    public void setDeletedFlag(Long deletedFlag) {
        this.deletedFlag = deletedFlag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
