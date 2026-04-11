package com.cariesguard.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.system.infrastructure.dataobject.SysRoleDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleDO> {

    @Select("""
            SELECT sr.role_code
            FROM sys_role sr
            INNER JOIN sys_user_role sur ON sr.id = sur.role_id
            WHERE sur.user_id = #{userId}
              AND sr.deleted_flag = 0
              AND sur.deleted_flag = 0
              AND sr.status = 'ACTIVE'
            ORDER BY sr.role_sort ASC
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
