package com.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.crm.common.result.Result;
import com.crm.dto.RecordCreateRequest;
import com.crm.dto.RecordQueryRequest;
import com.crm.service.RecordService;
import com.crm.vo.RecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 跟进记录接口
 */
@Tag(name = "06. 跟进记录", description = "客户/线索/商机三主体的统一时间轴")
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
}
