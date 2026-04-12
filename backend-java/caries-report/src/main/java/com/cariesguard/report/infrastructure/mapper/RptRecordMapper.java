package com.cariesguard.report.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.report.infrastructure.dataobject.RptRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RptRecordMapper extends BaseMapper<RptRecordDO> {
}

