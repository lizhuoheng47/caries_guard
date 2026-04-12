package com.cariesguard.report.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.report.infrastructure.dataobject.ReportCaseDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportCaseMapper extends BaseMapper<ReportCaseDO> {
}

