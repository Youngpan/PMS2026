package com.pms.module.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OTA 保活轮询与自动重连调度器（FR-04 / FR-06）。
 *
 * 定时任务：
 * - keepaliveTask: 每 60 秒扫描一次，对每个"已登录"账号执行保活轮询
 * - reconnectTask: 每 30 秒扫描一次，对"掉线"且到达重试时间的账号执行自动重连
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OtaKeepaliveScheduler {

    private final OtaAccountService otaAccountService;

    /**
     * FR-04：保活轮询
     * 每 60 秒扫描已登录账号，按各自 keepaliveInterval 判断是否需要保活。
     * 若连续 3 次保活失败则标记掉线（FR-05）。
     */
    @Scheduled(fixedRate = 60_000)
    public void keepaliveTask() {
        List<OtaAccount> accounts = otaAccountService.findAliveAccounts();
        if (accounts.isEmpty()) return;

        for (OtaAccount acc : accounts) {
            // 区分线程池（TODO: 按账号隔离线程池）
            boolean ok = otaAccountService.keepalive(acc);
            if (!ok) {
                // 连续失败次数通过 retryCount 来近似判断
                int fails = acc.getRetryCount() == null ? 0 : acc.getRetryCount();
                fails++;
                acc.setRetryCount(fails);
                // FR-05：连续 3 次保活失败 → 标记掉线
                if (fails >= 3) {
                    otaAccountService.markOffline(acc, "连续" + fails + "次保活失败");
                }
            }
        }
    }

    /**
     * FR-06：自动重连
     * 每 30 秒扫描需要重连的账号。
     */
    @Scheduled(fixedRate = 30_000)
    public void reconnectTask() {
        List<OtaAccount> accounts = otaAccountService.findReconnectAccounts();
        if (accounts.isEmpty()) return;

        for (OtaAccount acc : accounts) {
            try {
                otaAccountService.attemptAutoReconnect(acc);
            } catch (Exception e) {
                log.error("[OTA] 重连执行异常 accountId={}", acc.getId(), e);
            }
        }
    }
}
