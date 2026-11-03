package com.pms.module.ota;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ota")
@RequiredArgsConstructor
public class OtaPlatformConfigController {

    private final OtaPlatformConfigService configService;

    @GetMapping("/platform-configs")
    public Result<List<OtaPlatformConfig>> list(@RequestParam(required = false) String platform) {
        if (platform != null && !platform.isEmpty()) {
            return Result.ok(configService.listByPlatform(platform));
        }
        return Result.ok(configService.listAll());
    }

    @GetMapping("/platform-configs/{platform}/{configName}")
    public Result<String> getConfig(@PathVariable String platform, @PathVariable String configName) {
        String value = configService.getConfig(platform, configName);
        return value != null ? Result.ok(value) : Result.ok("");
    }
}
