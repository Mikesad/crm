package com.crm.config;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.crm.common.UserContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * MyBatis-Plus 数据权限拦截器
 *
 * <p>按当前用户的 {@code dataScope} 自动给受控表的查询 / 更新 / 删除 SQL 拼接 WHERE 条件：</p>
 * <ul>
 *   <li>1 全部 - 不加条件</li>
 *   <li>3 本部门 - {@code owner_user_id IN (SELECT id FROM sys_user WHERE dept_id = ?)}</li>
 *   <li>4 本部门及以下 - 同上，dept 集合包含子部门（按 sys_dept.ancestors）</li>
 *   <li>5 仅本人 - {@code owner_user_id = ?}</li>
 *   <li>2 自定义 - 暂按 5 处理，后续在 sys_role_custom_dept 表实现</li>
 * </ul>
 *
 * <p>读取 {@link UserContext} 中的 session 字段，<b>0 次 DB 命中</b>。</p>
 *
 * <p><b>生效范围</b>：仅对配置在 {@link #MANAGED_TABLES} 中的表生效。其他表（如 sys_user、sys_role）
 * 不加数据权限条件。如需扩展，修改该集合即可。</p>
 *
 * <p>类名特意避开 {@code DataPermissionHandler}（与 MyBatis-Plus 接口同名），用全限定名引用即可。</p>
 */
@Slf4j
@Component
public class CrmDataPermissionHandler implements DataPermissionHandler {

    /**
     * 需要数据权限拦截的业务表（物理表名）
     * <p>随阶段二业务模块上线扩充，与 crm_full.sql 真实表名保持一致。</p>
     */
    private static final Set<String> MANAGED_TABLES = new HashSet<>(Arrays.asList(
            "crm_customer",
            "crm_lead",
            "crm_business",
            "crm_contract",
            "crm_contact",
            "crm_record"
    ));

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // 1. 未登录 / 系统任务不处理
        Long userId = UserContext.currentUserId();
        if (userId == null) {
            return where;
        }

        // 2. 仅对受控表生效
        String table = resolveTable(mappedStatementId);
        if (table == null || !MANAGED_TABLES.contains(table)) {
            return where;
        }

        // 3. 按 dataScope 拼条件
        int scope = UserContext.currentDataScope();
        Expression scopeExpr = buildScopeExpression(scope);
        if (scopeExpr == null) {
            return where;
        }

        return where == null ? scopeExpr : new AndExpression(where, scopeExpr);
    }

    /**
     * 从 mappedStatementId 解析物理表名
     * <p>id 形如 {@code com.crm.mapper.CustomerMapper.selectList}，
     * 简化处理：取 mapper 类名，去 Mapper 后缀，转 snake_case。</p>
     */
    private String resolveTable(String msId) {
        if (msId == null) return null;
        int dot = msId.lastIndexOf('.');
        if (dot < 0) return null;
        String className = msId.substring(0, dot);
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 0) return null;
        String simpleName = className.substring(lastDot + 1);
        if (!simpleName.endsWith("Mapper")) return null;
        String raw = simpleName.substring(0, simpleName.length() - "Mapper".length());
        return camelToSnake(raw);
    }

    private static String camelToSnake(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) sb.append('_');
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private Expression buildScopeExpression(int scope) {
        Long userId = UserContext.currentUserId();
        Long deptId = UserContext.currentDeptId();
        try {
            switch (scope) {
                case 1:
                    return null;
                case 2:
                    log.warn("data_scope=2 (自定义) 暂未实现，按仅本人处理。请后续在 sys_role_custom_dept 表落地。");
                    return parse("owner_user_id = " + userId);
                case 3:
                    if (deptId == null) return parse("owner_user_id = " + userId);
                    return parse("owner_user_id IN (SELECT id FROM sys_user WHERE dept_id = " + deptId + ")");
                case 4:
                    if (deptId == null) return parse("owner_user_id = " + userId);
                    return parse("owner_user_id IN (SELECT id FROM sys_user WHERE dept_id IN " +
                            "(SELECT id FROM sys_dept WHERE id = " + deptId +
                            " OR FIND_IN_SET(" + deptId + ", ancestors)))");
                case 5:
                    return parse("owner_user_id = " + userId);
                default:
                    log.warn("未知 data_scope={}，按仅本人处理", scope);
                    return parse("owner_user_id = " + userId);
            }
        } catch (Exception e) {
            // 永不抛：拦截器异常会污染业务 SQL，降级为仅本人
            log.error("构建数据权限 SQL 片段失败，降级为仅本人", e);
            try {
                return parse("owner_user_id = " + userId);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private Expression parse(String condition) throws Exception {
        return CCJSqlParserUtil.parseCondExpression(condition);
    }
}