package com.pms.module.ota;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OTA 账号直连管理：携程/美团/抖音。
 */
@RestController
@RequestMapping("/ota/accounts")
@RequiredArgsConstructor
public class OtaAccountController {

    private final OtaAccountService otaAccountService;

    @GetMapping
    public Result<List<OtaAccount>> list(@RequestParam(required = false) String platform) {
        return Result.ok(otaAccountService.list(platform));
    }

    @PostMapping
    public Result<OtaAccount> create(@RequestBody OtaAccount acc) {
        return Result.ok(otaAccountService.create(acc));
    }

    @PutMapping("/{id}")
    public Result<OtaAccount> update(@PathVariable Long id, @RequestBody OtaAccount acc) {
        return Result.ok(otaAccountService.update(id, acc));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        otaAccountService.delete(id);
        return Result.ok();
    }

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

    /** 测试连接：校验账号密码并尝试登录 OTA 平台。 */
    @PostMapping("/{id}/test")
    public Result<OtaAccount> testConnection(@PathVariable Long id) {
        return Result.ok(otaAccountService.testConnection(id));
    }

    /** 手动同步：拉取该 OTA 账号的最新订单。 */
    @PostMapping("/{id}/sync")
    public Result<OtaAccount> sync(@PathVariable Long id) {
        return Result.ok(otaAccountService.sync(id));
    }
}
