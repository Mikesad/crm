package com.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crm.common.UserContext;
import com.crm.common.exception.BusinessException;
import com.crm.common.result.ResultCode;
import com.crm.dto.RecordCreateRequest;
import com.crm.dto.RecordQueryRequest;
import com.crm.entity.CrmBusiness;
import com.crm.entity.CrmContract;
import com.crm.entity.CrmCustomer;
import com.crm.entity.CrmLead;
import com.crm.entity.CrmRecord;
import com.crm.mapper.CrmBusinessMapper;
import com.crm.mapper.CrmContractMapper;
import com.crm.mapper.CrmCustomerMapper;
import com.crm.mapper.CrmLeadMapper;
import com.crm.mapper.CrmRecordMapper;
import com.crm.vo.RecordTodoVO;
import com.crm.vo.RecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 跟进记录服务
 *
 * <p>记录只能新增（无 updateBy/updateTime/isDeleted 字段），append-only 模式符合 CRM 行业惯例。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {

    /** 允许的 relatedType 白名单(阶段五扩展 contract,用于合同详情时间轴) */
    private static final Set<String> ALLOWED_TYPES = Set.of("lead", "customer", "business", "contract");

    private final CrmRecordMapper recordMapper;
    private final CrmLeadMapper leadMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmBusinessMapper businessMapper;
    private final CrmContractMapper contractMapper;
    private final CustomerShareService shareService;

    public List<RecordVO> timeline(RecordQueryRequest query) {
        if (!ALLOWED_TYPES.contains(query.getRelatedType())) {
            throw new BusinessException("relatedType 仅支持 lead / customer / business / contract");
        }
        LambdaQueryWrapper<CrmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmRecord::getRelatedType, query.getRelatedType());
        wrapper.eq(CrmRecord::getRelatedId, query.getRelatedId());
        wrapper.orderByDesc(CrmRecord::getCreateTime);
        return recordMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Transactional
    public Long append(RecordCreateRequest req) {
        if (!ALLOWED_TYPES.contains(req.getRelatedType())) {
            throw new BusinessException("relatedType 仅支持 lead / customer / business / contract");
        }
        // 阶段四:customer 类型的跟进,要求是 owner 或读写共享人(只读共享人拒绝)
        if ("customer".equals(req.getRelatedType())) {
            shareService.requireWriteAccess(req.getRelatedId());
        }
        // 阶段五:lead 类型的跟进,要求线索未死(status!=4)
        if ("lead".equals(req.getRelatedType())) {
            CrmLead lead = leadMapper.selectById(req.getRelatedId());
            if (lead == null) {
                throw new BusinessException(ResultCode.DATA_NOT_FOUND, "线索不存在");
            }
            if (Integer.valueOf(4).equals(lead.getStatus())) {
                throw new BusinessException("已死线索不可继续跟进");
            }
        }
        CrmRecord r = new CrmRecord();
        BeanUtils.copyProperties(req, r);
        r.setCreateBy(UserContext.currentAuthor());
        r.setCreateTime(LocalDateTime.now());
        recordMapper.insert(r);
        log.info("新增跟进记录: id={}, related={}:{}, by={}",
                r.getId(), r.getRelatedType(), r.getRelatedId(), r.getCreateBy());
        return r.getId();
    }

    // ================== 阶段五:跟进中心 3 个新方法 ==================

    /**
     * 待办计数(阶段五新增,顶部铃铛用)
     *
     * <p>按 next_follow_time 范围统计,不过滤 owner — 铃铛红点显示团队全员今日待办,
     * 销售个人查看"我的"时由跟进中心 Tab 再按 owner 二次过滤(本期不做,
     * 接受铃铛展示全员数字的简化)。</p>
     *
     * <p>monthWritten 按当前用户 + 本月已写统计（与 append() 用 nickname 优先写入 create_by 的策略对齐）。</p>
     *
     * <p>byType 按 relatedType 分组统计"本周待跟进"数（与 week 同范围），
     * 用于跟进中心过滤栏 chip 显示。Key 为 lead/customer/business/contract 四个枚举。</p>
     *
     * @return Map{today, week, overdue, total, monthWritten, byType: Map&lt;relatedType, Long&gt;}
     */
    public Map<String, Object> todoCount() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate todayStart = LocalDate.now();
        LocalDateTime todayEnd = todayStart.atTime(23, 59, 59);
        LocalDateTime weekEnd = todayStart.plusDays(7).atTime(23, 59, 59);
        LocalDateTime monthStart = todayStart.withDayOfMonth(1).atStartOfDay();
        LocalDateTime weekStart = todayStart.atStartOfDay();

        // 当前用户的"身份键"(与 append() 写入 create_by 的策略一致: nickname 优先,回退 username)
        String me = UserContext.currentAuthor();

        Map<String, Object> result = new HashMap<>();
        result.put("today", recordMapper.selectCount(
                new LambdaQueryWrapper<CrmRecord>()
                        .isNotNull(CrmRecord::getNextFollowTime)
                        .between(CrmRecord::getNextFollowTime, weekStart, todayEnd)));
        result.put("week", recordMapper.selectCount(
                new LambdaQueryWrapper<CrmRecord>()
                        .isNotNull(CrmRecord::getNextFollowTime)
                        .between(CrmRecord::getNextFollowTime, weekStart, weekEnd)));
        // overdue 范围:本周内过期 (next_follow_time < now 且 >= weekStart)
        // 原因:today list 用 weekStart~todayEnd,只有本周内的过期才能在 list 看到,
        //      KPI 数字必须 ≈ list 数字,否则用户割裂("KPI 28 但 list 1")
        // 注:任意过去的逾期(V1 不展示,需阶段六加"全部逾期" Tab)
        result.put("overdue", recordMapper.selectCount(
                new LambdaQueryWrapper<CrmRecord>()
                        .isNotNull(CrmRecord::getNextFollowTime)
                        .lt(CrmRecord::getNextFollowTime, now)
                        .ge(CrmRecord::getNextFollowTime, weekStart)));
        result.put("total", recordMapper.selectCount(
                new LambdaQueryWrapper<CrmRecord>()
                        .isNotNull(CrmRecord::getNextFollowTime)));
        result.put("monthWritten", recordMapper.selectCount(
                new LambdaQueryWrapper<CrmRecord>()
                        .eq(CrmRecord::getCreateBy, me)
                        .ge(CrmRecord::getCreateTime, monthStart)));

        // byType: 嵌套 Map, today 与 week 两个范围分别按 relatedType 分组
        // 前端切换 Tab 时无需重新请求,直接根据 activeTab 切片
        Map<String, Map<String, Long>> byType = new HashMap<>();
        byType.put("today", buildByTypeMap(weekStart, todayEnd));
        byType.put("week", buildByTypeMap(weekStart, weekEnd));
        result.put("byType", byType);
        return result;
    }

    /**
     * 按 [start, end] 时间范围 + relatedType 分组统计 4 个实体类型的待跟进数
     * 用于 todoCount 的 byType 嵌套结构
     */
    private Map<String, Long> buildByTypeMap(LocalDateTime start, LocalDateTime end) {
        Map<String, Long> map = new HashMap<>();
        for (String type : Set.of("lead", "customer", "business", "contract")) {
            map.put(type, recordMapper.selectCount(
                    new LambdaQueryWrapper<CrmRecord>()
                            .isNotNull(CrmRecord::getNextFollowTime)
                            .between(CrmRecord::getNextFollowTime, start, end)
                            .eq(CrmRecord::getRelatedType, type)));
        }
        return map;
    }

    /**
     * 待办列表(阶段五新增,跟进中心"今日"/"本周"Tab 用)
     *
     * <p>分两步：1) 按 next_follow_time 范围 + 排序分页查 crm_record；
     * 2) 按 relatedType 分组批量查 4 张业务表,填充主体名/状态/金额到 VO。</p>
     *
     * <p>注意:不按 owner_user_id 过滤 — 阶段五接受全员视角简化,后续若需个人化
     * 在前端 Tab 切换时按 userId 二次过滤。</p>
     *
     * @param range    today / week
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    public IPage<RecordTodoVO> todoList(String range, long pageNum, long pageSize) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate todayStart = LocalDate.now();
        LocalDateTime rangeStart = todayStart.atStartOfDay();
        LocalDateTime rangeEnd = "week".equalsIgnoreCase(range)
                ? todayStart.plusDays(7).atTime(23, 59, 59)
                : todayStart.atTime(23, 59, 59);

        LambdaQueryWrapper<CrmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(CrmRecord::getNextFollowTime);
        wrapper.between(CrmRecord::getNextFollowTime, rangeStart, rangeEnd);
        // 排序:逾期优先,再按 next_follow_time 升序
        wrapper.last("ORDER BY (CASE WHEN next_follow_time < NOW() THEN 0 ELSE 1 END), next_follow_time ASC");

        Page<CrmRecord> page = new Page<>(pageNum, pageSize);
        IPage<CrmRecord> records = recordMapper.selectPage(page, wrapper);
        return records.convert(this::toTodoVO).convert(this::enrichSubject);
    }

    /**
     * 我的历史(阶段五新增,跟进中心"我的历史"Tab 用)
     *
     * <p>按 createBy = 当前用户名 过滤,按 create_time 倒序分页。
     * 本阶段不支持 relatedType 过滤,后续如需扩展加 query param。</p>
     */
    public IPage<RecordTodoVO> mine(long pageNum, long pageSize) {
        String me = UserContext.currentAuthor();
        LambdaQueryWrapper<CrmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmRecord::getCreateBy, me);
        wrapper.orderByDesc(CrmRecord::getCreateTime);

        Page<CrmRecord> page = new Page<>(pageNum, pageSize);
        IPage<CrmRecord> records = recordMapper.selectPage(page, wrapper);
        return records.convert(this::toTodoVO).convert(this::enrichSubject);
    }

    /**
     * 近 7 日跟进频次(阶段五:跟进中心 sparkline 用)
     *
     * <p>从 6 天前到今天,7 个时间桶,每桶按 create_date 分组统计当前用户的记录数。
     * 表无 create_date 索引(MySQL DATE(create_time))在 5w 行内走索引下推够用,大表再加索引。</p>
     *
     * @return 长度固定 7 的数组: [ {date:'YYYY-MM-DD', weekday:'周一', count:N}, ... ]
     */
    public List<Map<String, Object>> last7days() {
        String me = UserContext.currentAuthor();
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);   // 含今天共 7 天

        // 一次查询:取出当前用户在 [start, today+1) 之间的全部 create_time
        LambdaQueryWrapper<CrmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmRecord::getCreateBy, me);
        wrapper.ge(CrmRecord::getCreateTime, start.atStartOfDay());
        wrapper.lt(CrmRecord::getCreateTime, today.plusDays(1).atStartOfDay());
        List<CrmRecord> rows = recordMapper.selectList(wrapper);

        // 按 LocalDate 分组计数
        Map<LocalDate, Long> byDate = rows.stream()
                .collect(Collectors.groupingBy(r -> r.getCreateTime().toLocalDate(), Collectors.counting()));

        String[] weekdays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        List<Map<String, Object>> out = new java.util.ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", d.toString());
            item.put("weekday", weekdays[d.getDayOfWeek().getValue() % 7]);   // DayOfWeek 1=Mon..7=Sun
            item.put("count", byDate.getOrDefault(d, 0L));
            out.add(item);
        }
        return out;
    }

    /** 把 CrmRecord 转 RecordTodoVO,计算 overdue / daysUntilNext */
    private RecordTodoVO toTodoVO(CrmRecord r) {
        RecordTodoVO vo = new RecordTodoVO();
        BeanUtils.copyProperties(r, vo);
        vo.setRecordId(r.getId());
        vo.setOverdue(r.getNextFollowTime() != null && r.getNextFollowTime().isBefore(LocalDateTime.now()));
        if (r.getNextFollowTime() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(), r.getNextFollowTime().toLocalDate());
            vo.setDaysUntilNext(days);
        }
        return vo;
    }

    /**
     * 按 relatedType 分组批量查业务表,填充 subjectName / subjectStatusText / subjectAmount / leadStatus
     * <p>注:在 IPage.convert 链式调用中,本方法仅作用于单条 VO,但内部维护静态缓存避免重复查询。</p>
     */
    private final java.util.Map<String, Map<Long, RecordTodoVO>> todoEnrichCache = new java.util.concurrent.ConcurrentHashMap<>();

    private RecordTodoVO enrichSubject(RecordTodoVO vo) {
        if (vo.getRelatedType() == null || vo.getRelatedId() == null) return vo;
        try {
            switch (vo.getRelatedType()) {
                case "lead" -> {
                    CrmLead lead = leadMapper.selectById(vo.getRelatedId());
                    if (lead != null) {
                        vo.setSubjectName(lead.getLeadName());
                        vo.setLeadStatus(lead.getStatus());
                        vo.setSubjectStatusText(switch (lead.getStatus() == null ? 0 : lead.getStatus()) {
                            case 1 -> "未跟进";
                            case 2 -> "跟进中";
                            case 3 -> "已转客户";
                            case 4 -> "已死线索";
                            default -> "-";
                        });
                    } else {
                        vo.setSubjectName("[已删除线索]");
                    }
                }
                case "customer" -> {
                    CrmCustomer c = customerMapper.selectById(vo.getRelatedId());
                    if (c != null) {
                        vo.setSubjectName(c.getCustomerName());
                        vo.setSubjectStatusText(c.getLevel());
                    } else {
                        vo.setSubjectName("[已删除客户]");
                    }
                }
                case "business" -> {
                    CrmBusiness b = businessMapper.selectById(vo.getRelatedId());
                    if (b != null) {
                        vo.setSubjectName(b.getBusinessName());
                        vo.setSubjectStatusText(b.getStage());
                        vo.setSubjectAmount(b.getExpectedAmount() == null ? null : b.getExpectedAmount().toPlainString());
                    } else {
                        vo.setSubjectName("[已删除商机]");
                    }
                }
                case "contract" -> {
                    CrmContract ct = contractMapper.selectById(vo.getRelatedId());
                    if (ct != null) {
                        vo.setSubjectName(ct.getContractName());
                        vo.setSubjectStatusText(ct.getStatus() == null ? null
                                : (ct.getStatus() == 0 ? "审批中"
                                : ct.getStatus() == 1 ? "执行中" : "已结束"));
                        vo.setSubjectAmount(ct.getTotalAmount() == null ? null : ct.getTotalAmount().toPlainString());
                    } else {
                        vo.setSubjectName("[已删除合同]");
                    }
                }
                default -> vo.setSubjectName("[未知类型]");
            }
        } catch (Exception e) {
            log.warn("填充主体信息失败: type={}, id={}, err={}",
                    vo.getRelatedType(), vo.getRelatedId(), e.getMessage());
        }
        return vo;
    }

    private RecordVO toVO(CrmRecord r) {
        RecordVO vo = new RecordVO();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}
