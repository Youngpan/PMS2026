package com.pms.ota;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.module.channel.Channel;
import com.pms.module.channel.ChannelMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 领域事件监听器：事务提交后把「待推送给 OTA」的事件落库到 interface_log。
 *
 * 当前仅记录（status=PENDING），不真正调用外部接口——签约/接渠道管理器前不发起任何出站请求。
 * 将来对接时，把这里替换为：按 channel 路由到对应 OTA/CM 的 HTTP 客户端推送，
 * 成功置 SUCCESS、失败置 FAILED 并重试。interface_log 即推送队列与审计。
 *
 * 房态/价格类事件 channel 为 null：广播给所有 is_ota=1 的渠道（携程/美团/飞猪…）。
 */
@Component
public class OtaEventListener {

    private static final Logger log = LoggerFactory.getLogger(OtaEventListener.class);

    private final InterfaceLogMapper interfaceLogMapper;
    private final ChannelMapper channelMapper;
    private final ObjectMapper objectMapper;

    public OtaEventListener(InterfaceLogMapper interfaceLogMapper, ChannelMapper channelMapper, ObjectMapper objectMapper) {
        this.interfaceLogMapper = interfaceLogMapper;
        this.channelMapper = channelMapper;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(OtaEvent e) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("refType", e.getRefType().name());
            body.put("refId", e.getRefId());
            body.put("action", e.getAction());
            if (e.getPayload() != null) body.put("data", e.getPayload());

            for (String channelCode : resolveChannels(e.getChannelId())) {
                InterfaceLog rec = new InterfaceLog();
                rec.setChannel(channelCode);
                rec.setDirection("out");
                rec.setPayload(objectMapper.writeValueAsString(body));
                rec.setStatus("PENDING");
                rec.setCreatedAt(LocalDateTime.now());
                interfaceLogMapper.insert(rec);
                log.info("OTA 事件已记录 [{}] {} {} ref={} channel={}", e.getRefType(), e.getAction(), e.getRefId(), channelCode);
            }
        } catch (Exception ex) {
            log.warn("记录 OTA 事件失败: {}", ex.getMessage());
        }
    }

    /** 单渠道事件→该渠道 code；广播事件→所有 is_ota=1 渠道 code。 */
    private List<String> resolveChannels(Long channelId) {
        if (channelId != null) {
            Channel c = channelMapper.selectById(channelId);
            return c != null ? List.of(c.getCode()) : List.of();
        }
        return channelMapper.selectList(new LambdaQueryWrapper<Channel>().eq(Channel::getIsOta, 1))
                .stream().map(Channel::getCode).toList();
    }
}
