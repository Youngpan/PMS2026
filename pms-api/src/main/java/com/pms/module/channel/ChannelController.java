package com.pms.module.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 渠道来源查询：供建单下拉、日历渠道展示复用。
 */
@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelMapper channelMapper;

    @GetMapping
    public Result<List<Channel>> list() {
        return Result.ok(channelMapper.selectList(new LambdaQueryWrapper<Channel>()
                .orderByAsc(Channel::getId)));
    }
}
