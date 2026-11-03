package com.pms.module.ota;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OTA 定时拉单调度器（PRD 4.2 PollingScheduler）。
 *
 * 定时任务：
 * - pollTask: 每 60 秒扫描一次，对每个CONNECTED账号增量拉取订单
 * - sessionRefreshTask: 每 2 小时执行一次，对每个CONNECTED账号重新登录刷新Cookie（FR-03 凭证刷新策略）
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OtaPollingScheduler {

    private final OtaAccountService otaAccountService;

    /**
     * 定时拉单：每 60 秒执行一次
     * 扫描所有 CONNECTED 账号，调用 fetchOrders 进行增量拉取
     */
    @Scheduled(fixedRate = 60_000)
    public void pollTask() {
        List<OtaAccount> accounts = otaAccountService.findAliveAccounts();
        if (accounts.isEmpty()) return;

        for (OtaAccount acc : accounts) {
            try {
                otaAccountService.fetchOrders(acc.getId());
            } catch (Exception e) {
                log.error("[OtaPolling] 拉单异常 accountId={}, err={}", acc.getId(), e.getMessage());
            }
        }
    }

    /**
     * FR-03：定时凭证刷新
     * 每 2 小时对已登录账号重新调用登录接口获取新 Cookie
     */
    @Scheduled(fixedRate = 7200_000)
    public void sessionRefreshTask() {
        List<OtaAccount> accounts = otaAccountService.findAliveAccounts();
        if (accounts.isEmpty()) return;

        for (OtaAccount acc : accounts) {
            try {
                otaAccountService.login(acc.getId());
                log.info("[OtaPolling] 定时刷新Cookie成功 accountId={}", acc.getId());
            } catch (Exception e) {
                log.warn("[OtaPolling] 定时刷新Cookie失败 accountId={}, err={}", acc.getId(), e.getMessage());
            }
        }
    }
}
