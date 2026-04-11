package com.cariesguard.system.infrastructure.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysMenuMapper {

    @Select("""
            SELECT DISTINCT sm.permission_code
            FROM sys_menu sm
            INNER JOIN sys_role_menu srm ON sm.id = srm.menu_id
            INNER JOIN sys_user_role sur ON srm.role_id = sur.role_id
            INNER JOIN sys_role sr ON sr.id = sur.role_id
            WHERE sur.user_id = #{userId}
              AND sm.deleted_flag = 0
              AND sm.status = 'ACTIVE'
              AND sm.permission_code IS NOT NULL
              AND sm.permission_code <> ''
              AND srm.deleted_flag = 0
              AND sur.deleted_flag = 0
              AND sr.deleted_flag = 0
              AND sr.status = 'ACTIVE'
            ORDER BY sm.permission_code ASC
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
