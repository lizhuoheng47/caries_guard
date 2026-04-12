package com.cariesguard.patient.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("pat_profile")
public class PatProfileDO {

    @TableId(type = IdType.INPUT)
    private Long id;
    private Long patientId;
    private Integer brushingFreqPerDay;
    private String sugarDietLevelCode;
    private String fluorideUseFlag;
    private String familyCariesHistoryFlag;
    private String orthodonticHistoryFlag;
    private Integer previousCariesCount;
    private Integer lastDentalCheckMonths;
    private String smokingFlag;
    private String drinkingFlag;
    private String oralHygieneLevelCode;
    private String allergyInfo;
    private String chronicDiseaseDesc;
    private String profileSourceCode;
    private LocalDate effectiveDate;
    private String extJson;
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
    public Integer getBrushingFreqPerDay() { return brushingFreqPerDay; }
    public void setBrushingFreqPerDay(Integer brushingFreqPerDay) { this.brushingFreqPerDay = brushingFreqPerDay; }
    public String getSugarDietLevelCode() { return sugarDietLevelCode; }
    public void setSugarDietLevelCode(String sugarDietLevelCode) { this.sugarDietLevelCode = sugarDietLevelCode; }
    public String getFluorideUseFlag() { return fluorideUseFlag; }
    public void setFluorideUseFlag(String fluorideUseFlag) { this.fluorideUseFlag = fluorideUseFlag; }
    public String getFamilyCariesHistoryFlag() { return familyCariesHistoryFlag; }
    public void setFamilyCariesHistoryFlag(String familyCariesHistoryFlag) { this.familyCariesHistoryFlag = familyCariesHistoryFlag; }
    public String getOrthodonticHistoryFlag() { return orthodonticHistoryFlag; }
    public void setOrthodonticHistoryFlag(String orthodonticHistoryFlag) { this.orthodonticHistoryFlag = orthodonticHistoryFlag; }
    public Integer getPreviousCariesCount() { return previousCariesCount; }
    public void setPreviousCariesCount(Integer previousCariesCount) { this.previousCariesCount = previousCariesCount; }
    public Integer getLastDentalCheckMonths() { return lastDentalCheckMonths; }
    public void setLastDentalCheckMonths(Integer lastDentalCheckMonths) { this.lastDentalCheckMonths = lastDentalCheckMonths; }
    public String getSmokingFlag() { return smokingFlag; }
    public void setSmokingFlag(String smokingFlag) { this.smokingFlag = smokingFlag; }
    public String getDrinkingFlag() { return drinkingFlag; }
    public void setDrinkingFlag(String drinkingFlag) { this.drinkingFlag = drinkingFlag; }
    public String getOralHygieneLevelCode() { return oralHygieneLevelCode; }
    public void setOralHygieneLevelCode(String oralHygieneLevelCode) { this.oralHygieneLevelCode = oralHygieneLevelCode; }
    public String getAllergyInfo() { return allergyInfo; }
    public void setAllergyInfo(String allergyInfo) { this.allergyInfo = allergyInfo; }
    public String getChronicDiseaseDesc() { return chronicDiseaseDesc; }
    public void setChronicDiseaseDesc(String chronicDiseaseDesc) { this.chronicDiseaseDesc = chronicDiseaseDesc; }
    public String getProfileSourceCode() { return profileSourceCode; }
    public void setProfileSourceCode(String profileSourceCode) { this.profileSourceCode = profileSourceCode; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getExtJson() { return extJson; }
    public void setExtJson(String extJson) { this.extJson = extJson; }
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
