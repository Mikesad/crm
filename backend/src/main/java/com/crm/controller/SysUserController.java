package com.crm.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.common.result.Result;
import com.crm.entity.SysUser;
import com.crm.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统用户接口
 *
 * <p>阶段四新增:为客户共享对话框提供"被共享人"下拉数据源。
 * 轻量接口,仅返回 id/username/nickname/deptId 字段,无敏感信息。</p>
 */
@Tag(name = "99. 系统 - 用户", description = "为前端选择器提供轻量用户列表")
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserMapper userMapper;

    @Operation(summary = "查询用户列表(供前端下拉/选择器用)")
    @SaIgnore  // 已登录即可,无独立权限码;由 Sa-Token 拦截器兜底
    @GetMapping("/list")
    public Result<List<SysUserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword));
        }
        if (deptId != null) {
            wrapper.eq(SysUser::getDeptId, deptId);
        }
        wrapper.orderByAsc(SysUser::getId).last("LIMIT 200");
        List<SysUser> users = userMapper.selectList(wrapper);
        return Result.success(users.stream().map(SysUserVO::of).toList());
    }

    /**
     * 轻量用户 VO,只暴露前端需要的字段,不含密码
     */
    @lombok.Data
    public static class SysUserVO {
        private Long id;
        private String username;
        private String nickname;
        private Long deptId;

        public static SysUserVO of(SysUser u) {
            SysUserVO vo = new SysUserVO();
            BeanUtil.copyProperties(u, vo);
            return vo;
        }
    }
}
