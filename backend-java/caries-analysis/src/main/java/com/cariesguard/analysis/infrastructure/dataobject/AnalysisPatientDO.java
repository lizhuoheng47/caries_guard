package com.cariesguard.analysis.infrastructure.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pat_patient")
public class AnalysisPatientDO {

    private Long id;
    private Integer age;
    private String genderCode;
    private String status;
    private Long deletedFlag;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGenderCode() { return genderCode; }
    public void setGenderCode(String genderCode) { this.genderCode = genderCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(Long deletedFlag) { this.deletedFlag = deletedFlag; }
}
