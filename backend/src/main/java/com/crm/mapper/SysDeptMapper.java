package com.crm.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门 Mapper
 *
 * <p>阶段七 commit:负责部门树查询、子节点/后代查询、删除前引用校验。</p>
 *
 * <p>阶段八 commit 2 扩展:新增 {@link #selectDescendantIds(Long)} 用于报表部门筛选认子部门(C2-D6)。</p>
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

    /**
     * 查询某部门及其所有后代部门 ID(阶段八 commit 7·2026-06-30 修复)
     * <p>使用 {@code FIND_IN_SET(rootId, ancestors) > 0 OR id = rootId};
     * 比 LIKE 更稳 — 修复原 LIKE ',X,' 对 {@code ancestors} 末尾 X 漏匹配的问题
     * (例:dept 5 ancestors='0,1,2' 用原 LIKE ',2,' 漏掉,FIND_IN_SET 正确)。</p>
     *
     * <p>典型用法:销售总监选"销售部"(id=2) → 自动包含"华东组(5)"/"华南组(6)"等所有子部门。</p>
     */
    @Select("""
            SELECT id
              FROM sys_dept
             WHERE (id = #{rootId} OR FIND_IN_SET(#{rootId}, ancestors) > 0)
               AND is_deleted = 0
            """)
    List<Long> selectDescendantIdsRaw(@Param("rootId") Long rootId);

    /**
     * default 包装(保持原签名兼容已有调用方)
     */
    default List<Long> selectDescendantIds(@Param("rootId") Long rootId) {
        if (rootId == null) return Collections.emptyList();
        return selectDescendantIdsRaw(rootId);
    }
}
