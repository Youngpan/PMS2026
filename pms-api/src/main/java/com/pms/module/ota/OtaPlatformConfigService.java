package com.pms.module.ota;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtaPlatformConfigService {

    private final OtaPlatformConfigMapper configMapper;

    public List<OtaPlatformConfig> listByPlatform(String platform) {
        return configMapper.selectList(new LambdaQueryWrapper<OtaPlatformConfig>()
                .eq(platform != null && !platform.isEmpty(), OtaPlatformConfig::getPlatform, platform));
    }

    public List<OtaPlatformConfig> listAll() {
        return configMapper.selectList(null);
    }

    public String getConfig(String platform, String configName) {
        OtaPlatformConfig cfg = configMapper.selectOne(
                new LambdaQueryWrapper<OtaPlatformConfig>()
                        .eq(OtaPlatformConfig::getPlatform, platform)
                        .eq(OtaPlatformConfig::getConfigName, configName));
        return cfg != null ? cfg.getConfigValue() : null;
    }
}
