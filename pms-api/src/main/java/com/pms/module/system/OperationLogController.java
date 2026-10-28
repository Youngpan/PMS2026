package com.pms.module.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.PageResult;
import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogMapper operationLogMapper;

    @GetMapping
    public Result<PageResult<OperationLog>> page(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "20") long page_size,
                                                 @RequestParam(required = false) Long staff_id,
                                                 @RequestParam(required = false) String module) {
        Page<OperationLog> p = new Page<>(page, page_size);
        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<OperationLog>()
                .eq(staff_id != null, OperationLog::getStaffId, staff_id)
                .eq(module != null, OperationLog::getModule, module)
                .orderByDesc(OperationLog::getCreatedAt);
        operationLogMapper.selectPage(p, w);
        return Result.ok(PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize()));
    }
}
