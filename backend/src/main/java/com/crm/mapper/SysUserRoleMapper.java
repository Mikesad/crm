package com.crm.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联 Mapper
 *
 * <p>对应 {@code sys_user_role} 表（联合主键 user_id + role_id）。
 * 该表结构简单无 entity,直接用 @Insert/@Select/@Delete 操作。</p>
 */
@Mapper
public interface SysUserRoleMapper {

    /**
     * 查询用户绑定的全部 role_id
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 查询绑定某角色的全部 user_id
     */
    @Select("SELECT user_id FROM sys_user_role WHERE role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 统计某角色绑定的用户数
     */
    @Select("SELECT COUNT(*) FROM sys_user_role WHERE role_id = #{roleId}")
    int countByRoleId(@Param("roleId") Long roleId);

    /**
     * 物理删除某用户的所有角色绑定
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 物理删除某用户的某一条角色绑定(阶段六 commit 1 收尾)
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 批量插入(v0.3 收尾改:用 INSERT IGNORE 兼容已存在的 (user_id, role_id) 联合主键对,
     * 避免 addMembers 时重复绑定抛 DuplicateKey)
     */
    @Insert(
        "<script>" +
        "INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES " +
        "<foreach collection='roleIds' item='rid' separator=','>" +
        "  (#{userId}, #{rid})" +
        "</foreach>" +
        "</script>"
    )
    int batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
