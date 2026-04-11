package com.cariesguard.system.infrastructure.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysDataPermissionRuleMapper {

    @Select("""
            SELECT DISTINCT sdp.scope_type_code
            FROM sys_data_permission_rule sdp
            INNER JOIN sys_user_role sur ON sdp.role_id = sur.role_id
            INNER JOIN sys_role sr ON sr.id = sur.role_id
            WHERE sur.user_id = #{userId}
              AND sdp.module_code = #{moduleCode}
              AND sdp.deleted_flag = 0
              AND sdp.status = 'ACTIVE'
              AND sur.deleted_flag = 0
              AND sr.deleted_flag = 0
              AND sr.status = 'ACTIVE'
            """)
    List<String> selectModuleScopeTypesByUserIdAndModule(@Param("userId") Long userId, @Param("moduleCode") String moduleCode);

    @Select("""
            SELECT sdp.dept_ids_json
            FROM sys_data_permission_rule sdp
            INNER JOIN sys_user_role sur ON sdp.role_id = sur.role_id
            INNER JOIN sys_role sr ON sr.id = sur.role_id
            WHERE sur.user_id = #{userId}
              AND sdp.module_code = #{moduleCode}
              AND sdp.scope_type_code = 'CUSTOM'
              AND sdp.deleted_flag = 0
              AND sdp.status = 'ACTIVE'
              AND sur.deleted_flag = 0
              AND sr.deleted_flag = 0
              AND sr.status = 'ACTIVE'
            """)
    List<String> selectCustomDeptIdsJsonByUserIdAndModule(@Param("userId") Long userId, @Param("moduleCode") String moduleCode);

    @Select("""
            SELECT DISTINCT sr.data_scope_code
            FROM sys_role sr
            INNER JOIN sys_user_role sur ON sr.id = sur.role_id
            WHERE sur.user_id = #{userId}
              AND sur.deleted_flag = 0
              AND sr.deleted_flag = 0
              AND sr.status = 'ACTIVE'
            """)
    List<String> selectRoleDataScopesByUserId(@Param("userId") Long userId);
}
