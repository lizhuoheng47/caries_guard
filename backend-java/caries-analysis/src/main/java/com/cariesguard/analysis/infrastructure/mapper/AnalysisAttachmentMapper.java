package com.cariesguard.analysis.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.analysis.infrastructure.dataobject.AnalysisAttachmentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalysisAttachmentMapper extends BaseMapper<AnalysisAttachmentDO> {
}
