package com.cariesguard.patient.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.patient.infrastructure.dataobject.MedCaseToothRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedCaseToothRecordMapper extends BaseMapper<MedCaseToothRecordDO> {
}
