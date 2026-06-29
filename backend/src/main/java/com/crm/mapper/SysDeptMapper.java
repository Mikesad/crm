package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 部门 Mapper
 *
 * <p>阶段七 commit:负责部门树查询、子节点/后代查询、删除前引用校验。</p>
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询某部门的直接子部门(parent_id = #{deptId})
     */
    @Select("""
            SELECT *
              FROM sys_dept
             WHERE parent_id  = #{deptId}
               AND is_deleted = 0
             ORDER BY order_num ASC, id ASC
            """)
    List<SysDept> selectChildrenById(@Param("deptId") Long deptId);

    /**
     * 查询某节点的所有后代(包括自己),通过 ancestors 前缀匹配
     */
    @Select("""
            SELECT *
              FROM sys_dept
             WHERE ancestors LIKE CONCAT(#{ancestorsPrefix}, '%')
               AND is_deleted = 0
             ORDER BY ancestors ASC, order_num ASC
            """)
    List<SysDept> selectDescendantsByAncestors(@Param("ancestorsPrefix") String ancestorsPrefix);

    /**
     * 同级下 deptName 唯一性校验(排除自己)
     *
     * @param parentId  上级部门 ID
     * @param deptName  待校验部门名称
     * @param excludeId 排除的部门 ID(更新时传自己)
     * @return 命中的记录数,0 表示无重名
     */
    @Select("""
            SELECT COUNT(*)
              FROM sys_dept
             WHERE parent_id  = #{parentId}
               AND dept_name  = #{deptName}
               AND is_deleted = 0
               AND id <> #{excludeId}
            """)
    long countByParentAndName(@Param("parentId") Long parentId,
                              @Param("deptName") String deptName,
                              @Param("excludeId") Long excludeId);

    /**
     * 部门下用户数(status=1 启用账号)
     */
    @Select("""
            SELECT COUNT(*)
              FROM sys_user
             WHERE dept_id   = #{deptId}
               AND is_deleted = 0
               AND status     = 1
            """)
    long countActiveUsersByDeptId(@Param("deptId") Long deptId);
}
