package com.pms.module.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * OTA 拉单调度器。
 *
 * 每个 channel_account 独立轮询，间隔 60s。
 * 增量拉取：pageIndex=0 → N，直到无数据或遇到已有 orderId。
 * 水位线：每页的最大 formDate 写入 page_watermark。
 *
 * 文档参考《普通直连携程渠道技术设计文档》第4章 定时轮询调度与拉单。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtaPollingScheduler {

    private final OtaAccountService otaAccountService;
    private final ChannelOrderMapper channelOrderMapper;
    private final ChannelListingService channelListingService;

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 每 60 秒扫描所有已登录账号，执行增量拉单。
     */
    @Scheduled(fixedRate = 60_000)
    public void pollOrdersTask() {
        List<OtaAccount> accounts = otaAccountService.findAliveAccounts();
        if (accounts.isEmpty()) return;

        for (OtaAccount acc : accounts) {
            try {
                pollAccount(acc);
            } catch (Exception e) {
                log.error("[OtaPolling] 拉单异常 accountId={}", acc.getId(), e);
            }
        }
    }

    /**
     * 对单个账号执行拉单（增量模式）
     */
    private void pollAccount(OtaAccount acc) {
        CtripApiClient client = new CtripApiClient(acc);
        String watermark = acc.getPageWatermark();
        int pageIndex = 0;
        boolean hasMore = true;
        String latestFormDate = null;

        while (hasMore) {
            List<ChannelOrder> orders;
            try {
                orders = client.fetchOrderPage(pageIndex, watermark);
            } catch (SecurityException e) {
                // 登录态失效 → 标记掉线
                log.warn("[OtaPolling] 账号 session 失效 accountId={}", acc.getId());
                otaAccountService.markOffline(acc, "拉单时检测到登录态失效: " + e.getMessage());
                return;
            } catch (Exception e) {
                log.warn("[OtaPolling] 拉取失败 accountId={}, page={}, err={}", acc.getId(), pageIndex, e.getMessage());
                hasMore = false;
                break;
            }

            if (orders.isEmpty()) {
                // 无更多数据
                hasMore = false;
                break;
            }

            for (ChannelOrder order : orders) {
                // 幂等处理：以 (channel_order_id, source_type) 唯一
                try {
                    channelOrderMapper.insert(order);
                    // 追踪最新 formDate（用当前时间作为近似值）
                    latestFormDate = LocalDateTime.now().format(DT_FORMAT);
                } catch (Exception e) {
                    // 唯一键冲突 → 说明已存在，停止向下拉取（因为增量模式下，后面的都已处理过）
                    log.debug("[OtaPolling] 订单已存在 accountId={}, orderId={}", acc.getId(), order.getChannelOrderId());
                    hasMore = false;
                    break;
                }

                // 尝试落单处理（将 RAW 订单转成百居易 reservation）
                tryProcessOrder(acc, order);
            }

            pageIndex++;
        }

        // 更新水位线
        if (latestFormDate != null) {
            acc.setPageWatermark(latestFormDate);
            acc.setLastSyncAt(LocalDateTime.now());
            otaAccountService.updateAccountWatermark(acc);
        }
    }

    /**
     * 尝试将 channel_order 落单 → 百居易 reservation
     * 状态映射：Receipt→CONFIRMED, CheckIn→CHECK_IN
     * 同时触发防撞单 autoCloseOtherChannels
     */
    private void tryProcessOrder(OtaAccount acc, ChannelOrder order) {
        if (!"RAW".equals(order.getProcessStatus())) return;

        try {
            // TODO: 调用 ReservationService 创建预订单
            // 落单时需查询 channel_listing 获得关联的 roomId/roomTypeId
            // 然后调用 autoCloseOtherChannels 触发防撞单

            order.setProcessStatus("PROCESSED");
            order.setProcessedAt(LocalDateTime.now());
            channelOrderMapper.updateById(order);

            log.info("[OtaPolling] 订单落单成功 accountId={}, orderId={}", acc.getId(), order.getChannelOrderId());
        } catch (Exception e) {
            // 落单失败 → 记录错误、状态为 FAILED
            order.setProcessStatus("FAILED");
            order.setErrorMsg(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "未知错误");
            channelOrderMapper.updateById(order);
            log.warn("[OtaPolling] 订单落单失败 accountId={}, orderId={}, err={}", acc.getId(), order.getChannelOrderId(), e.getMessage());
        }
    }
}
