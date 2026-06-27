package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper
 *
 * <p>登录流程用 {@link #selectActiveRolesByUserId} 一次性取回该用户所有启用角色。</p>
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 查询某用户的所有启用角色（仅 status=1 且未逻辑删除）
     */
    @Select("""
            SELECT r.*
              FROM sys_role r
              JOIN sys_user_role ur ON ur.role_id = r.id
             WHERE ur.user_id = #{userId}
               AND r.status   = 1
               AND r.is_deleted = 0
            """)
    List<SysRole> selectActiveRolesByUserId(@Param("userId") Long userId);
}