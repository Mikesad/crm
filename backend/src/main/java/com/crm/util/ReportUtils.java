package com.crm.util;

import java.math.BigDecimal;

/**
 * 报表中心工具类（阶段五 commit 2）
 *
 * <p>聚合 SQL 的 SUM / COUNT 结果经 MyBatis-Plus {@code selectMaps} 取出为 {@code Object}
 * (实际是 {@code BigDecimal} / {@code Long} / {@code Integer}),需要规整为本项目统一
 * 的字符串形式(BigDecimal.toPlainString,避免科学计数法)。</p>
 *
 * <p>不做 BigDecimal → String 之外的格式化(精度/小数位/千分位),前端按需展示。</p>
 */
public final class ReportUtils {

    private ReportUtils() {}

    /**
     * 把 SELECT SUM/COUNT 返回的 Object 安全转为 BigDecimal。
     * <p>常见输入: BigDecimal / Long / Integer / String, 统一走 BigDecimal 构造。</p>
     *
     * @param obj 聚合结果(可能为 null)
     * @return BigDecimal(空值返回 ZERO,避免 NPE)
     */
    public static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(obj.toString());
    }

    /**
     * BigDecimal → String(防科学计数法)
     */
    public static String toPlainString(BigDecimal v) {
        return v == null ? null : v.toPlainString();
    }

    /**
     * 安全转 Long
     */
    public static Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(obj.toString());
    }
}
