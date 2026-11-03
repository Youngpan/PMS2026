package com.pms.module.ota;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 渠道房源产品管理 REST 接口。
 */
@RestController
@RequestMapping("/ota")
@RequiredArgsConstructor
public class ChannelProductController {

    private final ChannelProductService channelProductService;

    /** 查询产品列表 */
    @GetMapping("/products")
    public Result<List<ChannelProduct>> list(
            @RequestParam(required = false) Long otaAccountId,
            @RequestParam(required = false) String platform) {
        return Result.ok(channelProductService.list(otaAccountId, platform));
    }

    /** 查询产品详情 */
    @GetMapping("/products/{id}")
    public Result<ChannelProduct> get(@PathVariable Long id) {
        return Result.ok(channelProductService.getById(id));
    }

    /** 创建产品 */
    @PostMapping("/products")
    public Result<ChannelProduct> create(@RequestBody ChannelProduct product) {
        return Result.ok(channelProductService.create(product));
    }

    /** 更新产品 */
    @PutMapping("/products/{id}")
    public Result<ChannelProduct> update(@PathVariable Long id, @RequestBody ChannelProduct product) {
        return Result.ok(channelProductService.update(id, product));
    }

    /** 切换产品在售/停售状态 */
    @PutMapping("/products/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody String status) {
        channelProductService.toggleStatus(id, status);
        return Result.ok();
    }

    /** 删除产品 */
    @DeleteMapping("/products/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        channelProductService.deleteById(id);
        return Result.ok();
    }
}
