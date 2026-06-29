package com.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.crm.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * <p>登录查询（按用户名）走 {@link BaseMapper#selectOne}，由 Service 层包装。</p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
