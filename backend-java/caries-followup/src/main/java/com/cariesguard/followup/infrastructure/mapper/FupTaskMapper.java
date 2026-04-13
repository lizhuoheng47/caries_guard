package com.cariesguard.followup.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.followup.infrastructure.dataobject.FupTaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FupTaskMapper extends BaseMapper<FupTaskDO> {
}
