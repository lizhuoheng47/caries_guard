package com.cariesguard.report.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.report.infrastructure.dataobject.ReportToothRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportToothRecordMapper extends BaseMapper<ReportToothRecordDO> {
}
