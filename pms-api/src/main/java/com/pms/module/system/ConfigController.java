package com.pms.module.system;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public Result<Hotel> get() {
        return Result.ok(configService.get());
    }

    @PutMapping
    public Result<Hotel> update(@RequestBody Hotel patch) {
        return Result.ok(configService.update(patch));
    }
}
