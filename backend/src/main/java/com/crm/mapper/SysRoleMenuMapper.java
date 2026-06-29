package com.crm.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-菜单关联 Mapper
 *
 * <p>对应 {@code sys_role_menu} 表（联合主键 role_id + menu_id）。
 * 与 {@link SysUserRoleMapper} 同风格,仅补充批量查询/物理删除等扩展。</p>
 */
@Mapper
public interface SysRoleMenuMapper {

    /**
     * 查询某角色绑定的全部 menu_id
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 物理删除某角色的所有菜单绑定
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 统计某菜单被多少角色绑定
     */
    @Select("SELECT COUNT(*) FROM sys_role_menu WHERE menu_id = #{menuId}")
    int countByMenuId(@Param("menuId") Long menuId);

    /**
     * 批量插入
     */
    @org.apache.ibatis.annotations.Insert(
        "<script>" +
        "INSERT INTO sys_role_menu(role_id, menu_id) VALUES " +
        "<foreach collection='menuIds' item='mid' separator=','>" +
        "  (#{roleId}, #{mid})" +
        "</foreach>" +
        "</script>"
    )
    int batchInsert(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
