package com.cariesguard.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.system.infrastructure.dataobject.SysUserRoleDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRoleDO> {

    @Select("""
            SELECT role_id
            FROM sys_user_role
            WHERE user_id = #{userId}
              AND deleted_flag = 0
            ORDER BY id ASC
            """)
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}
