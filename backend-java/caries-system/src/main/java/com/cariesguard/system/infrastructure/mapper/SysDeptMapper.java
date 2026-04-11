package com.cariesguard.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.system.infrastructure.dataobject.SysDeptDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDeptDO> {
}
