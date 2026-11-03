package com.pms.module.ota;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 渠道房源关联 + 冲突订单盒子。
 */
@RestController
@RequestMapping("/ota")
@RequiredArgsConstructor
public class ChannelListingController {

    private final ChannelListingService channelListingService;

    // ==================== 房源关联 ====================

    /** 查询某 OTA 账号下的所有房源关联 */
    @GetMapping("/accounts/{accountId}/listings")
    public Result<List<ChannelListing>> listByAccount(@PathVariable Long accountId) {
        return Result.ok(channelListingService.listByAccount(accountId));
    }

    /** 查询所有已关联的房源列表（含 OTA 账号信息） */
    @GetMapping("/listings/all")
    public Result<List<ChannelListing>> listAll() {
        return Result.ok(channelListingService.listAll());
    }

    /** 查询关联到某房间的所有渠道房源 */
    @GetMapping("/listings/by-room/{roomId}")
    public Result<List<ChannelListing>> listByRoom(@PathVariable Long roomId) {
        return Result.ok(channelListingService.listByRoom(roomId));
    }

    /** 查询关联到某房型的所有渠道房源 */
    @GetMapping("/listings/by-room-type/{roomTypeId}")
    public Result<List<ChannelListing>> listByRoomType(@PathVariable Long roomTypeId) {
        return Result.ok(channelListingService.listByRoomType(roomTypeId));
    }

    /** 建立房源关联 */
    @PostMapping("/listings")
    public Result<ChannelListing> bind(@RequestBody ChannelListing listing) {
        return Result.ok(channelListingService.bind(listing));
    }

    /** 解除房源关联 */
    @DeleteMapping("/listings/{id}")
    public Result<Void> unbind(@PathVariable Long id) {
        channelListingService.unbind(id);
        return Result.ok();
    }

    // ==================== 冲突订单盒子 ====================

    /** 查询待处理冲突订单 */
    @GetMapping("/conflicts/pending")
    public Result<List<ConflictOrder>> listPendingConflicts() {
        return Result.ok(channelListingService.listPendingConflicts());
    }

    /** 处理冲突订单 */
    @PutMapping("/conflicts/{id}/resolve")
    public Result<ConflictOrder> resolveConflict(@PathVariable Long id, @RequestBody ConflictOrder order) {
        return Result.ok(channelListingService.resolveConflict(id, order.getResolution(), order.getStatus()));
    }
}
