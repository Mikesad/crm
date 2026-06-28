package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.crm.common.UserContext;
import com.crm.entity.SysUser;
import com.crm.mapper.SysUserMapper;
import com.crm.util.ReportUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报表中心查询辅助(阶段五 commit 2)
 *
 * <p>承担 3 个跨 Service 复用的纯计算职责:</p>
 * <ol>
 *   <li>时间范围 → [start, end] 闭区间:range=today/week/month/quarter/year/custom</li>
 *   <li>部门/人员筛选 → ownerIds 列表:deptId → sys_user 查 → 用户 ID 集合;
 *       userId 单独传时仅含自己</li>
 *   <li>同比字符串:本次值 vs 上次值,产出 "↑ 18.6%" / "↓ 3.2%" / null</li>
 * </ol>
 *
 * <p>不涉及 SQL,纯 Java 逻辑,不进缓存。</p>
 */
@Component
@RequiredArgsConstructor
public class ReportQueryHelper {

    private final SysUserMapper sysUserMapper;

    /**
     * 把 range 字符串转成 [start, end] 闭区间,Service 层据此切片。
     *
     * @param range     today / week / month / quarter / year / custom
     * @param startDate 自定义开始,range=custom 时必填(yyyy-MM-dd)
     * @param endDate   自定义结束,range=custom 时必填(yyyy-MM-dd)
     * @return [start, end] 闭区间(含两端)
     */
    public LocalDateTime[] resolveRange(String range, String startDate, String endDate) {
        LocalDate today = LocalDate.now();
        LocalDateTime[] result = new LocalDateTime[2];

        switch (range == null ? "month" : range.toLowerCase()) {
            case "today" -> {
                result[0] = today.atStartOfDay();
                result[1] = today.atTime(23, 59, 59);
            }
            case "week" -> {
                result[0] = today.minusDays(6).atStartOfDay();
                result[1] = today.atTime(23, 59, 59);
            }
            case "quarter" -> {
                int q = (today.getMonthValue() - 1) / 3;
                result[0] = LocalDate.of(today.getYear(), q * 3 + 1, 1).atStartOfDay();
                result[1] = today.atTime(23, 59, 59);
            }
            case "year" -> {
                result[0] = LocalDate.of(today.getYear(), 1, 1).atStartOfDay();
                result[1] = today.atTime(23, 59, 59);
            }
            case "custom" -> {
                if (startDate == null || endDate == null) {
                    throw new IllegalArgumentException("range=custom 时 startDate / endDate 必填");
                }
                result[0] = LocalDate.parse(startDate).atStartOfDay();
                result[1] = LocalDate.parse(endDate).atTime(23, 59, 59);
            }
            case "month" -> {
                result[0] = today.withDayOfMonth(1).atStartOfDay();
                result[1] = today.atTime(23, 59, 59);
            }
            default -> {
                result[0] = today.withDayOfMonth(1).atStartOfDay();
                result[1] = today.atTime(23, 59, 59);
            }
        }
        return result;
    }

    /**
     * 解析部门/人员筛选 → ownerIds 列表。
     * <p>规则:</p>
     * <ul>
     *   <li>userId 优先:返回 [userId](单元素)</li>
     *   <li>deptId 选中:查 sys_user.dept_id=?,返回所有 user.id</li>
     *   <li>都为空:返回 null(代表"全部",由 Mapper 走无条件)</li>
     * </ul>
     *
     * @param deptId 部门 ID(可空)
     * @param userId 用户 ID(可空,优先级最高)
     * @return ownerIds 集合(null=全部;空集合=无匹配)
     */
    public List<Long> resolveOwnerIds(Long deptId, Long userId) {
        if (userId != null) {
            return List.of(userId);
        }
        if (deptId != null) {
            List<SysUser> users = sysUserMapper.selectList(
                    new QueryWrapper<SysUser>().select("id").eq("dept_id", deptId).eq("status", 1));
            if (users.isEmpty()) return Collections.emptyList();
            return users.stream().map(SysUser::getId).collect(Collectors.toList());
        }
        return null;   // null = 全部
    }

    /**
     * 拼缓存 key(全 Service 共用)
     */
    public String cacheKey(String reportKey, String range, Long deptId, Long userId) {
        return reportKey + ":" + (range == null ? "month" : range) + ":d" + deptId + ":u" + userId;
    }

    /**
     * 拼同比字符串(如 "↑ 18.6%" / "↓ 3.2%" / null)。
     *
     * @param current 本期值
     * @param previous 同期值(上月/上年)
     * @param isPercent 是否按百分点(pp)展示,默认 false(按 % 展示)
     * @return "↑ 18.6%" / "↓ 3.2%" / null(previous=0 时返回 null 避免除零)
     */
    public String computeDelta(java.math.BigDecimal current, java.math.BigDecimal previous, boolean isPercent) {
        if (current == null || previous == null) return null;
        if (previous.compareTo(java.math.BigDecimal.ZERO) == 0) return null;
        java.math.BigDecimal diff = current.subtract(previous);
        java.math.BigDecimal rate = diff.multiply(new java.math.BigDecimal(100))
                .divide(previous, 2, java.math.RoundingMode.HALF_UP);
        String sign = diff.compareTo(java.math.BigDecimal.ZERO) >= 0 ? "↑ " : "↓ ";
        return sign + rate.abs().toPlainString() + (isPercent ? " pp" : "%");
    }

    /**
     * 拼同比方向(up / down / null)
     */
    public String computeDeltaDir(java.math.BigDecimal current, java.math.BigDecimal previous) {
        if (current == null || previous == null) return null;
        int cmp = current.compareTo(previous);
        if (cmp > 0) return "up";
        if (cmp < 0) return "down";
        return null;
    }

    /**
     * 工具:BigDecimal 安全转 String
     */
    public static String str(java.math.BigDecimal v) {
        return ReportUtils.toPlainString(v);
    }
}
