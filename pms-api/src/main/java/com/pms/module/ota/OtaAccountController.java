package com.pms.module.ota;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OTA 普通直连登录态管理（PRD FR-01 ~ FR-12）。
 * 接口路径：/ota/accounts
 */
@RestController
@RequestMapping("/ota/accounts")
@RequiredArgsConstructor
public class OtaAccountController {

    private final OtaAccountService otaAccountService;

    /** FR-01：账号列表 */
    @GetMapping
    public Result<List<OtaAccount>> list(@RequestParam(required = false) String platform) {
        return Result.ok(otaAccountService.list(platform));
    }

    /** FR-01：新增并绑定（首次绑定直接登录） */
    @PostMapping
    public Result<OtaAccount> bind(@RequestBody OtaAccount acc) {
        return Result.ok(otaAccountService.bind(acc));
    }

    /** 更新账号信息 */
    @PutMapping("/{id}")
    public Result<OtaAccount> update(@PathVariable Long id, @RequestBody OtaAccount acc) {
        return Result.ok(otaAccountService.update(id, acc));
    }

    /** FR-11：重新登录 */
    @PostMapping("/{id}/login")
    public Result<OtaAccount> login(@PathVariable Long id) {
        return Result.ok(otaAccountService.login(id));
    }

    /** FR-12：解绑 */
    @DeleteMapping("/{id}")
    public Result<Void> unbind(@PathVariable Long id) {
        otaAccountService.unbind(id);
        return Result.ok();
    }

    /** 启用/停用 */
    @PutMapping("/{id}/enabled")
    public Result<OtaAccount> toggleEnabled(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer enabled;
        Object raw = body.get("enabled");
        if (raw == null) {
            enabled = null;
        } else {
            enabled = Integer.valueOf(raw.toString());
        }
        return Result.ok(otaAccountService.toggleEnabled(id, enabled));
    }

    /** FR-07：更新风控配置 */
    @PutMapping("/{id}/server-config")
    public Result<OtaAccount> updateServerConfig(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(otaAccountService.updateServerConfig(id, body.get("config")));
    }

    /** FR-09：读取订单（手动即时同步） */
    @PostMapping("/{id}/fetch-orders")
    public Result<OtaAccount> fetchOrders(@PathVariable Long id) {
        return Result.ok(otaAccountService.fetchOrders(id));
    }

    /** FR-02：全量同步（房源关联后触发） */
    @PostMapping("/{id}/full-sync")
    public Result<OtaAccount> fullSync(@PathVariable Long id) {
        return Result.ok(otaAccountService.fullSync(id));
    }
}
