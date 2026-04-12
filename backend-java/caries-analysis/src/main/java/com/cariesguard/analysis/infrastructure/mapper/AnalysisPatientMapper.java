package com.cariesguard.analysis.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.analysis.infrastructure.dataobject.AnalysisPatientDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalysisPatientMapper extends BaseMapper<AnalysisPatientDO> {
}
