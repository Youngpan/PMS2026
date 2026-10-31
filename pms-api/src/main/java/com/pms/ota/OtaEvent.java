package com.pms.ota;

import java.util.Map;

/**
 * PMS 领域事件：订单/房态/价格变更。服务层只负责发布，不关心下游如何处理。
 * 监听器（OtaEventListener）在事务提交后记录到 interface_log，
 * 将来接渠道管理器或直连携程时，把监听器换成真实 HTTP 推送即可。
 */
public class OtaEvent {
    public enum RefType { RESERVATION, ROOM_AVAILABILITY, ROOM_RATE }

    private final RefType refType;
    private final Long refId;
    private final String action;        // CREATED/CONFIRMED/CHECKED_IN/CHECKED_OUT/CANCELLED/BLOCKED/RELEASED/RENT_CHANGED/CLEAN_CHANGED/RATE_CHANGED ...
    private final Long channelId;       // 关联渠道（OTA 推送路由用）；房态/价格类事件可为 null（广播给所有 OTA）
    private final Map<String, Object> payload;

    public OtaEvent(RefType refType, Long refId, String action, Long channelId, Map<String, Object> payload) {
        this.refType = refType;
        this.refId = refId;
        this.action = action;
        this.channelId = channelId;
        this.payload = payload;
    }

    public RefType getRefType() { return refType; }
    public Long getRefId() { return refId; }
    public String getAction() { return action; }
    public Long getChannelId() { return channelId; }
    public Map<String, Object> getPayload() { return payload; }
}
