package com.cariesguard.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cariesguard.system.infrastructure.dataobject.SysRoleMenuDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenuDO> {

    @Select("""
            SELECT menu_id
            FROM sys_role_menu
            WHERE role_id = #{roleId}
              AND deleted_flag = 0
            ORDER BY id ASC
            """)
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
