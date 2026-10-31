package com.pms.ota;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 领域事件发布器：业务服务通过它发布「可推送给 OTA」的事件。
 * 解耦：服务不知道是否真的对接了 OTA，只声明发生了什么。
 */
@Component
public class OtaEventPublisher {

    private final ApplicationEventPublisher publisher;

    public OtaEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(OtaEvent.RefType refType, Long refId, String action, Long channelId, Map<String, Object> payload) {
        publisher.publishEvent(new OtaEvent(refType, refId, action, channelId, payload));
    }
}
