package com.crm.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.SysMenuCreateRequest;
import com.crm.dto.SysMenuUpdateRequest;
import com.crm.entity.SysMenu;
import com.crm.mapper.SysMenuMapper;
import com.crm.mapper.SysRoleMenuMapper;
import com.crm.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 系统菜单服务
 *
 * <p>核心约束：</p>
 * <ul>
 *   <li>menuType=M 目录:perms 必空</li>
 *   <li>menuType=C 菜单:path + component 必填</li>
 *   <li>menuType=F 按钮:perms 必填</li>
 *   <li>删除前校验:无子菜单 + 无角色绑定</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    /** 内置 5 角色不可删的核心菜单(防止误删 sys:* 关键能力) */
    private static final Set<String> PROTECTED_MENU_PERMS = Set.of(
            "sys:system:view",
            "sys:user:list", "sys:role:list", "sys:menu:list", "sys:dept:list"
    );

    /**
     * 查询全量菜单(平铺,前端自行组装树)
     */
    public List<SysMenuVO> listAll() {
        List<SysMenu> all = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getOrderNum, SysMenu::getId));
        return all.stream().map(this::toVO).toList();
    }

    /**
     * 查询全量菜单并组装成树
     */
    public List<SysMenuVO> tree() {
        List<SysMenuVO> all = listAll();
        return buildTree(all, 0L);
    }

    public SysMenuVO detail(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "菜单不存在");
        }
        return toVO(menu);
    }

    @Transactional
    public Long create(SysMenuCreateRequest req) {
        validateMenuType(req.getMenuType(), req.getPath(), req.getComponent(), req.getPerms());
        SysMenu menu = new SysMenu();
        BeanUtil.copyProperties(req, menu);
        if (req.getParentId() == null) menu.setParentId(0L);
        if (req.getStatus() == null) menu.setStatus(1);
        if (req.getOrderNum() == null) menu.setOrderNum(0);
        menuMapper.insert(menu);
        log.info("创建菜单: id={}, name={}, perms={}", menu.getId(), menu.getMenuName(), menu.getPerms());
        return menu.getId();
    }

    @Transactional
    public void update(SysMenuUpdateRequest req) {
        SysMenu menu = menuMapper.selectById(req.getId());
        if (menu == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "菜单不存在");
        }
        // 类型/必填字段校验
        if (req.getMenuType() != null) {
            validateMenuType(req.getMenuType(),
                    req.getPath() != null ? req.getPath() : menu.getPath(),
                    req.getComponent() != null ? req.getComponent() : menu.getComponent(),
                    req.getPerms() != null ? req.getPerms() : menu.getPerms());
        }
        if (StringUtils.hasText(req.getMenuName())) menu.setMenuName(req.getMenuName());
        if (req.getParentId() != null) menu.setParentId(req.getParentId());
        if (req.getOrderNum() != null) menu.setOrderNum(req.getOrderNum());
        if (req.getPath() != null) menu.setPath(req.getPath());
        if (req.getComponent() != null) menu.setComponent(req.getComponent());
        if (req.getMenuType() != null) menu.setMenuType(req.getMenuType());
        if (req.getPerms() != null) menu.setPerms(req.getPerms());
        if (req.getStatus() != null) menu.setStatus(req.getStatus());
        menuMapper.updateById(menu);
        log.info("更新菜单: id={}", menu.getId());
    }

    @Transactional
    public void delete(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "菜单不存在");
        }
        // 保护:关键能力菜单不可删
        if (StringUtils.hasText(menu.getPerms()) && PROTECTED_MENU_PERMS.contains(menu.getPerms())) {
            throw new BusinessException("关键能力菜单「" + menu.getMenuName() + "」不可删除");
        }
        // 校验无子菜单
        Long childCount = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("存在子菜单,不能删除");
        }
        // 校验无角色绑定
        int roleBindCount = roleMenuMapper.countByMenuId(id);
        if (roleBindCount > 0) {
            throw new BusinessException("存在角色绑定此菜单,不能删除");
        }
        menuMapper.deleteById(id);  // sys_menu 无 is_deleted,走物理删
        log.info("删除菜单: id={}", id);
    }

    private void validateMenuType(String type, String path, String component, String perms) {
        if (type == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "menuType 不能为空");
        }
        switch (type) {
            case "M" -> {
                if (StringUtils.hasText(perms)) {
                    throw new BusinessException("目录类型(M)的 perms 必须为空");
                }
            }
            case "C" -> {
                if (!StringUtils.hasText(path) || !StringUtils.hasText(component)) {
                    throw new BusinessException("菜单类型(C)的 path 和 component 必填");
                }
            }
            case "F" -> {
                if (!StringUtils.hasText(perms)) {
                    throw new BusinessException("按钮类型(F)的 perms 必填");
                }
            }
            default -> throw new BusinessException("menuType 只能为 M/C/F");
        }
    }

    private SysMenuVO toVO(SysMenu menu) {
        SysMenuVO vo = BeanUtil.copyProperties(menu, SysMenuVO.class);
        vo.setMenuTypeText(menuTypeText(menu.getMenuType()));
        vo.setStatusText(menu.getStatus() != null && menu.getStatus() == 1 ? "显示" : "隐藏");
        return vo;
    }

    private String menuTypeText(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case "M" -> "目录";
            case "C" -> "菜单";
            case "F" -> "按钮";
            default -> "未知";
        };
    }

    private List<SysMenuVO> buildTree(List<SysMenuVO> all, Long parentId) {
        List<SysMenuVO> roots = new ArrayList<>();
        for (SysMenuVO vo : all) {
            if (parentId.equals(vo.getParentId())) {
                List<SysMenuVO> children = buildTree(all, vo.getId());
                vo.setChildren(children);
                roots.add(vo);
            }
        }
        return roots;
    }
}
