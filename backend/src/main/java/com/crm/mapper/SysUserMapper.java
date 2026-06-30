package com.crm.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户 Mapper
 *
 * <p>登录查询（按用户名）走 {@link BaseMapper#selectOne}，由 Service 层包装。</p>
 *
 * <p>阶段八 commit 2 扩展:新增 {@link #selectIdByDeptIds(List)} 给报表按部门筛选用(C2-D6)。</p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按部门 ID 列表批量查 user.id(阶段八 commit 2·C2-D6)
     * <p>仅返回 id,用于聚合查询的 IN 列表;status=1 启用、is_deleted=0。</p>
     * <p>用 MyBatis-Plus wrapper:deptIds 为空时直接返回空集合,避免 IN () 报错。</p>
     */
    default List<Long> selectIdByDeptIds(@Param("deptIds") List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return Collections.emptyList();
        List<SysUser> rows = selectList(new QueryWrapper<SysUser>()
                .select("id")
                .in("dept_id", deptIds)
                .eq("status", 1)
                .eq("is_deleted", 0));
        return rows.stream().map(SysUser::getId).collect(Collectors.toList());
    }
}