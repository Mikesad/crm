package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.crm.common.result.Result;
import com.crm.dto.RecordCreateRequest;
import com.crm.dto.RecordQueryRequest;
import com.crm.service.RecordService;
import com.crm.vo.RecordTodoVO;
import com.crm.vo.RecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 跟进记录接口
 */
@Tag(name = "06. 跟进记录", description = "客户/线索/商机/合同四主体的统一时间轴 + 跟进中心待办/历史")
@RestController
@RequestMapping("/crm/record")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @Operation(summary = "拉取时间轴",
        description = "按 relatedType + relatedId 拉取关联主体的全部跟进记录，按时间倒序")
    @SaCheckPermission("crm:record:list")
    @GetMapping("/timeline")
    public Result<List<RecordVO>> timeline(@Valid RecordQueryRequest query) {
        return Result.success(recordService.timeline(query));
    }

    @Operation(summary = "新增跟进",
        description = "追加一条跟进记录。线索转客户 / 商机阶段推进等业务变更也会自动埋点，无需手动调此接口")
    @SaCheckPermission("crm:record:add")
    @PostMapping
    public Result<Long> append(@Valid @RequestBody RecordCreateRequest req) {
        return Result.success(recordService.append(req));
    }

    // ================== 阶段五:跟进中心 3 个新接口 ==================

    @Operation(summary = "待办数量统计(顶部铃铛用)",
        description = "按 next_follow_time 范围返回 {today, week, overdue, total, monthWritten, byType} 6 个字段,byType 为 Map<relatedType, Long> 分组统计")
    @SaCheckPermission("crm:record:center")
    @GetMapping("/todo/count")
    public Result<Map<String, Object>> todoCount() {
        return Result.success(recordService.todoCount());
    }

    @Operation(summary = "待办列表(跟进中心今日/本周 Tab)",
        description = "按 range=today|week + 分页查询;排序:逾期优先,再按 next_follow_time 升序")
    @SaCheckPermission("crm:record:center")
    @GetMapping("/todo/list")
    public Result<IPage<RecordTodoVO>> todoList(
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(recordService.todoList(range, pageNum, pageSize));
    }

    @Operation(summary = "我的历史(跟进中心'我的历史' Tab)",
        description = "按 create_by=当前用户 过滤,按 create_time 倒序分页")
    @SaCheckPermission("crm:record:center")
    @GetMapping("/mine")
    public Result<IPage<RecordTodoVO>> mine(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(recordService.mine(pageNum, pageSize, keyword));
    }

    @Operation(summary = "近 7 日跟进频次(跟进中心 sparkline)",
        description = "返回长度为 7 的数组,从 6 天前到今天;每项 {date, weekday, count};按 create_by 过滤当前用户")
    @SaCheckPermission("crm:record:center")
    @GetMapping("/stats/last7days")
    public Result<List<Map<String, Object>>> last7days() {
        return Result.success(recordService.last7days());
    }
}
