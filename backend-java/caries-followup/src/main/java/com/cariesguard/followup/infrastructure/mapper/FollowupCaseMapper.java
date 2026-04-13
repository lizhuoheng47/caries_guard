package com.cariesguard.followup.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.followup.infrastructure.dataobject.FollowupCaseDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FollowupCaseMapper extends BaseMapper<FollowupCaseDO> {
}
