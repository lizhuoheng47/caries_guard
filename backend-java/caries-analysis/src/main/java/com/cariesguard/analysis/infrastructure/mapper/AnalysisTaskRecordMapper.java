package com.cariesguard.analysis.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.analysis.infrastructure.dataobject.AnaTaskRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalysisTaskRecordMapper extends BaseMapper<AnaTaskRecordDO> {
}
