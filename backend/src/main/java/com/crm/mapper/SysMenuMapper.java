package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 / 功能权限 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询某用户的所有功能权限码（去重，仅 perms 非空）
     */
    @Select("""
            SELECT DISTINCT m.perms
              FROM sys_menu m
              JOIN sys_role_menu rm ON rm.menu_id = m.id
              JOIN sys_user_role ur ON ur.role_id = rm.role_id
             WHERE ur.user_id  = #{userId}
               AND m.status    = 1
               AND m.perms    <> ''
               AND m.perms IS NOT NULL
            """)
    List<String> selectPermsByUserId(@Param("userId") Long userId);
}