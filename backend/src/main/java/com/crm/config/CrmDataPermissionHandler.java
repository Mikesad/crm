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
 *   <li>3 本部门组 - {@code owner_user_id IN (SELECT id FROM sys_user WHERE dept_id IN
 *       (SELECT id FROM sys_dept WHERE id = ? OR parent_id = (SELECT parent_id FROM sys_dept WHERE id = ?)))}
 *       <br>即"我的部门 + 同 parent_id 的所有兄弟部门"</li>
 *   <li>5 仅本人 - {@code owner_user_id = ?}（阶段四起 crm_customer 扩展为
 *       {@code owner_user_id = ? OR id IN (crm_customer_share) OR is_public = 1}）</li>
 *   <li>2 自定义 - 暂按 5 处理，后续在 sys_role_custom_dept 表实现</li>
 * </ul>
 *
 * <p>phase8 commit1 修订：删除原 scope=4『本部门及以下』档。理由：与 scope=3『本部门组』在叶子部门
 * 上等价（叶子无子部门），反而让 sales_director 必须依赖一档现已无意义的值。sales_director 改用 scope=3，
 * 在 dept=总公司 时 parent_id=0 的兄弟部门覆盖全部顶级部门，等价于"看全部销售"语义。</p>
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
     * <p>阶段四修复:crm_contact / crm_record 移出 — 这俩表无 owner_user_id 字段,
     * 拦截器强拼 owner_user_id=X 会报 Unknown column SQL 错误。
     * 改为"信任父表"模型:你能看父表(客户/线索/商机),就能看其 contact/record
     * (contact 强依赖 customerId,record 强依赖 related_id+related_type,
     * 父表已被本拦截器过滤,子表跟着走)。</p>
     *
     * <p>仍不含(无 owner_user_id): crm_receivable / crm_receivable_plan,
     * 权限靠 @SaCheckPermission 兜底;receivable 通过 contract 间接走数据权限。</p>
     */
    private static final Set<String> MANAGED_TABLES = new HashSet<>(Arrays.asList(
            "crm_customer",
            "crm_lead",
            "crm_business",
            "crm_contract"
            // V1 不含: crm_contact(无 owner_user_id,信任父 customer),
            //          crm_record(无 owner_user_id,信任 related_id 对应的父表),
            //          crm_receivable / crm_receivable_plan
            // phase8 commit1: crm_approval 表已拆掉
    ));

    /**
     * 阶段四:共享子查询只在 crm_customer 表生效。
     * <p>其他受控表(lead/business/contract/contact/record) 维持原 owner_user_id 逻辑。</p>
     */
    private static final String TABLE_CUSTOMER = "crm_customer";

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
        Expression scopeExpr = buildScopeExpression(scope, table);
        if (scopeExpr == null) {
            return where;
        }

        // 4. 关键:把 scopeExpr 用括号包住,避免 AND 优先级被外部 OR 偷走
        //    例如:WHERE id=? AND is_deleted=0 AND (a OR b) 才能保证整个 OR 块在 AND 之下
        //    否则会变成:WHERE id=? AND is_deleted=0 AND a OR b — b 漏到顶层
        try {
            Expression parened = parse("(" + scopeExpr.toString() + ")");
            return where == null ? parened : new AndExpression(where, parened);
        } catch (Exception e) {
            log.error("数据权限 scopeExpr 包裹括号失败,降级为直接拼接", e);
            return where == null ? scopeExpr : new AndExpression(where, scopeExpr);
        }
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

    private Expression buildScopeExpression(int scope, String table) {
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
                    // 本部门组:我的部门 + 同 parent_id 的所有兄弟部门
                    if (deptId == null) return parse("owner_user_id = " + userId);
                    return parse("owner_user_id IN (SELECT id FROM sys_user WHERE dept_id IN " +
                            "(SELECT id FROM sys_dept WHERE id = " + deptId +
                            " OR parent_id = (SELECT parent_id FROM (SELECT * FROM sys_dept) d WHERE d.id = " + deptId + ")))");
                case 5:
                    return buildScopeForReadonly(userId, table);
                default:
                    // 含历史 data_scope=4(本部门及以下,phase8 拆档) — 一律兜底为仅本人
                    log.warn("未知或已废弃 data_scope={}，按仅本人处理", scope);
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

    /**
     * 阶段四:dataScope=5 的 WHERE 拼装
     * <p>对 crm_customer 扩展为"自己 OR 被共享 OR 公海";其他表维持原 owner_user_id = ? 逻辑。</p>
     */
    private Expression buildScopeForReadonly(Long userId, String table) throws Exception {
        if (TABLE_CUSTOMER.equals(table)) {
            // 共享子查询 + 公海放行
            String sharedSub = "id IN (SELECT customer_id FROM crm_customer_share WHERE user_id = " + userId + ")";
            return parse(
                    "owner_user_id = " + userId
                  + " OR " + sharedSub
                  + " OR is_public = 1"
            );
        }
        return parse("owner_user_id = " + userId);
    }

    private Expression parse(String condition) throws Exception {
        return CCJSqlParserUtil.parseCondExpression(condition);
    }
}