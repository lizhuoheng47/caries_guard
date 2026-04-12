package com.cariesguard.patient.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.patient.infrastructure.dataobject.MedCaseStatusLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedCaseStatusLogMapper extends BaseMapper<MedCaseStatusLogDO> {
}
