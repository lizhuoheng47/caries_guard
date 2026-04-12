package com.cariesguard.patient.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.patient.infrastructure.dataobject.AnaResultSummaryDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnaResultSummaryMapper extends BaseMapper<AnaResultSummaryDO> {
}
