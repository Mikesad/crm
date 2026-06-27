package com.crm.service;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.crm.common.SessionKeys;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 鉴权接口实现
 *
 * <p>由 Sa-Token 在每次 {@code @SaCheckPermission} / {@code @SaCheckRole} 校验时调用。
 * 这里 <b>不查数据库</b>，全部从登录时写入的当前请求 tokenSession 直接读。</p>
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    /**
     * 返回指定 loginId 拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (!StpUtil.isLogin()) {
            return Collections.emptyList();
        }
        Object perms = StpUtil.getTokenSession().get(SessionKeys.PERMISSIONS);
        return perms == null ? Collections.emptyList() : (List<String>) perms;
    }

    /**
     * 返回指定 loginId 拥有的角色 key 集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if (!StpUtil.isLogin()) {
            return Collections.emptyList();
        }
        Object roles = StpUtil.getTokenSession().get(SessionKeys.ROLE_KEYS);
        return roles == null ? Collections.emptyList() : (List<String>) roles;
    }
}