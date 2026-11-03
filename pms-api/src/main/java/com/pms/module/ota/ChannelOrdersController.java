package com.pms.module.ota;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 渠道订单拉取暂存管理。
 * 路径：/ota/orders
 */
@RestController
@RequestMapping("/ota/orders")
@RequiredArgsConstructor
public class ChannelOrdersController {

    private final ChannelOrderMapper channelOrderMapper;

    /** 订单列表 */
    @GetMapping
    public Result<List<ChannelOrder>> list(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String processStatus) {
        LambdaQueryWrapper<ChannelOrder> w = new LambdaQueryWrapper<ChannelOrder>()
                .eq(platform != null && !platform.isEmpty(), ChannelOrder::getPlatform, platform)
                .eq(processStatus != null && !processStatus.isEmpty(), ChannelOrder::getProcessStatus, processStatus)
                .orderByDesc(ChannelOrder::getCreatedAt);
        return Result.ok(channelOrderMapper.selectList(w));
    }

    /** 查看单条订单详情 */
    @GetMapping("/{id}")
    public Result<ChannelOrder> get(@PathVariable Long id) {
        ChannelOrder order = channelOrderMapper.selectById(id);
        if (order == null) return Result.fail(404, "订单不存在");
        return Result.ok(order);
    }

    /** 删除待处理的订单 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        channelOrderMapper.deleteById(id);
        return Result.ok();
    }
}
