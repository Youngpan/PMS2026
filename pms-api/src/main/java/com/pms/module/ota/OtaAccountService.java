package com.pms.module.ota;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.module.system.OperationLog;
import com.pms.module.system.OperationLogMapper;
import com.pms.security.StaffContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * OTA 普通直连登录态管理（PRD 覆盖 FR-01 ~ FR-12）。
 *
 * 核心机制：
 * 1. 账号绑定与首次登录（FR-01）
 * 2. 登录态凭证加密持久化（FR-03）
 * 3. 保活轮询（FR-04）—— 在名为 OtaKeepaliveScheduler 的调度器中触发
 * 4. 掉线检测（FR-05）
 * 5. 自动重连与指数退避（FR-06）
 * 6. 风控对抗策略（FR-07）
 * 7. 重新登录/重新授权（FR-11）
 * 8. 解绑与数据清理（FR-12）
 * 9. 读取订单手动同步（FR-09）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtaAccountService {

    private final OtaAccountMapper otaAccountMapper;
    private final OperationLogMapper operationLogMapper;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    // 加密密钥（实际生产应从密钥管理服务获取）
    // 32 hex 字符 = 16 字节 AES-128 密钥
    private final String AES_KEY_HEX = "706d73323032366f74616b6579736563";

    /**
     * FR-01：账号绑定（首次绑定保存凭证）
     * 前端提交账号密码 → 调用渠道登录接口 → 保存 session_token
     */
    public OtaAccount bind(OtaAccount acc) {
        validate(acc);
        try {
            acc.setHotelId(hotelId);
            acc.setStatus("CONNECTED");
            acc.setEnabled(1);
            acc.setAutoReconnect(1);
            acc.setRetryCount(0);
            acc.setKeepaliveInterval(300);
            acc.setOperatorId(currentOperator());
            acc.setCreatedAt(LocalDateTime.now());
            acc.setUpdatedAt(LocalDateTime.now());

            String rawToken = doChannelLogin(acc);
            String encrypted = encryptToken(rawToken);
            acc.setSessionToken(encrypted);
            acc.setSessionExpiresAt(LocalDateTime.now().plusHours(23));

            otaAccountMapper.insert(acc);
            auditLog("bind", acc.getId(), "平台=" + acc.getPlatform() + ", 账号=" + acc.getAccountName());
            log.info("[OTA] 账号绑定成功 id={}, platform={}, account={}", acc.getId(), acc.getPlatform(), acc.getAccountName());
            return acc;
        } catch (Exception e) {
            log.error("[OTA] 绑定失败", e);
            throw e;
        }
    }

    /**
     * FR-01：首次登录（绑定后的登录验证）
     * FR-11：重新登录入口
     */
    public OtaAccount login(Long id) {
        OtaAccount acc = otaAccountMapper.selectById(id);
        if (acc == null) throw new BusinessException("账号不存在");

        String rawToken = doChannelLogin(acc);
        String encrypted = encryptToken(rawToken);
        acc.setSessionToken(encrypted);
        acc.setSessionExpiresAt(LocalDateTime.now().plusHours(23));
        acc.setStatus("CONNECTED");
        acc.setOfflineAt(null);
        acc.setOfflineReason(null);
        acc.setLastError(null);
        acc.setRetryCount(0);
        acc.setNextRetryAt(null);
        acc.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.updateById(acc);

        auditLog("login", id, "重新登录成功");
        log.info("[OTA] 账号登录成功 id={}", id);
        return acc;
    }

    /**
     * FR-12：解绑与数据清理
     */
    public void unbind(Long id) {
        OtaAccount acc = otaAccountMapper.selectById(id);
        if (acc == null) throw new BusinessException("账号不存在");
        otaAccountMapper.deleteById(id);
        // session_token 随记录删除一并清除

        auditLog("unbind", id, "平台=" + acc.getPlatform() + ", 账号=" + acc.getAccountName());
        log.warn("[OTA] 账号解绑 id={}, platform={}, account={}", id, acc.getPlatform(), acc.getAccountName());
    }

    // ==================== 增删改（基础） ====================

    public List<OtaAccount> list(String platform) {
        LambdaQueryWrapper<OtaAccount> w = new LambdaQueryWrapper<OtaAccount>()
                .eq(OtaAccount::getHotelId, hotelId)
                .eq(platform != null && !platform.isEmpty(), OtaAccount::getPlatform, platform)
                .orderByDesc(OtaAccount::getCreatedAt);
        return otaAccountMapper.selectList(w);
    }

    public OtaAccount update(Long id, OtaAccount acc) {
        OtaAccount exist = otaAccountMapper.selectById(id);
        if (exist == null) throw new BusinessException("账号不存在");
        exist.setPlatform(acc.getPlatform());
        exist.setAccountName(acc.getAccountName());
        exist.setMerchantId(acc.getMerchantId());
        exist.setAppKey(acc.getAppKey());
        if (acc.getPassword() != null && !acc.getPassword().isEmpty()) {
            exist.setPassword(acc.getPassword());
        }
        if (acc.getAppSecret() != null && !acc.getAppSecret().isEmpty()) {
            exist.setAppSecret(acc.getAppSecret());
        }
        if (acc.getKeepaliveInterval() != null) {
            exist.setKeepaliveInterval(acc.getKeepaliveInterval());
        }
        if (acc.getAutoReconnect() != null) {
            exist.setAutoReconnect(acc.getAutoReconnect());
        }
        if (acc.getServerConfig() != null && !acc.getServerConfig().isEmpty()) {
            exist.setServerConfig(acc.getServerConfig());
        }
        exist.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.updateById(exist);
        return exist;
    }

    public OtaAccount toggleEnabled(Long id, Integer enabled) {
        OtaAccount acc = otaAccountMapper.selectById(id);
        if (acc == null) throw new BusinessException("账号不存在");
        acc.setEnabled(enabled);
        acc.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.updateById(acc);
        return acc;
    }

    // ==================== FR-04：保活轮询接口 ====================

    /**
     * 单次保活轮询：调用渠道轻量接口维持 session 活跃
     * 由 OtaKeepaliveScheduler 定时调用
     */
    public boolean keepalive(OtaAccount acc) {
        try {
            if (!"CONNECTED".equals(acc.getStatus())) {
                return false;
            }
            if (acc.getSessionToken() == null) {
                return false;
            }
            // 解密 token
            String token = decryptToken(acc.getSessionToken());
            // TODO: 调用渠道保活接口（如拉取最新1条订单或房态）
            doKeepaliveRequest(acc.getPlatform(), token);

            // 保活成功，重置重试计数器
            if (acc.getRetryCount() != null && acc.getRetryCount() > 0) {
                acc.setRetryCount(0);
                acc.setNextRetryAt(null);
                otaAccountMapper.updateById(acc);
            }
            return true;
        } catch (Exception e) {
            log.warn("[OTA] 保活失败 accountId={}, err={}", acc.getId(), e.getMessage());
            return false;
        }
    }

    // ==================== FR-05/FR-06：掉线检测与自动重连 ====================

    /**
     * FR-05：标记掉线（由 KeepaliveScheduler 在检测到 401/连续失败时调用）
     */
    public void markOffline(OtaAccount acc, String reason) {
        acc.setStatus("OFFLINE");
        acc.setOfflineAt(LocalDateTime.now());
        acc.setOfflineReason(reason);
        acc.setLastError(reason);

        if (acc.getAutoReconnect() != null && acc.getAutoReconnect() == 1) {
            // FR-06：开启自动重连，先尝试第一次重试
            scheduleAutoReconnect(acc);
        }
        otaAccountMapper.updateById(acc);

        log.warn("[OTA] 账号掉线 id={}, reason={}, autoReconnect={}",
                acc.getId(), reason, acc.getAutoReconnect());
        // FR-10：掉线通知（当前输出日志，真实场景发送站内+微信通知）
        notifyOffline(acc);
    }

    /**
     * FR-06：执行自动重连
     * 由 KeepaliveScheduler 定时调用（检查 nextRetryAt）
     */
    public boolean attemptAutoReconnect(OtaAccount acc) {
        if (acc.getAutoReconnect() == null || acc.getAutoReconnect() != 1) {
            return false;
        }
        if (!"OFFLINE".equals(acc.getStatus())) {
            return false;
        }

        try {
            String rawToken = doChannelLogin(acc);
            String encrypted = encryptToken(rawToken);
            acc.setSessionToken(encrypted);
            acc.setSessionExpiresAt(LocalDateTime.now().plusHours(23));
            acc.setStatus("CONNECTED");
            acc.setOfflineAt(null);
            acc.setOfflineReason(null);
            acc.setLastError(null);
            acc.setRetryCount(0);
            acc.setNextRetryAt(null);
            acc.setUpdatedAt(LocalDateTime.now());
            otaAccountMapper.updateById(acc);

            log.info("[OTA] 自动重连成功 id={}", acc.getId());
            // 重连成功后触发全量同步（占位）
            triggerFullSync(acc);
            return true;
        } catch (Exception e) {
            // 失败：退避+重试
            int retry = acc.getRetryCount() == null ? 0 : acc.getRetryCount();
            retry++;
            acc.setRetryCount(retry);
            acc.setLastError("重连失败: " + e.getMessage());
            acc.setUpdatedAt(LocalDateTime.now());

            if (retry >= 5) {
                // 超过重试阈值（5次），停止自动重连，标记需人工
                acc.setAutoReconnect(0);
                acc.setNextRetryAt(null);
                log.warn("[OTA] 自动重连超阈值 id={}, retry={}, 需人工介入", acc.getId(), retry);
            } else {
                // 指数退避：2^retry 分钟（30s, 1min, 2min, 4min, 8min）
                long delayMinutes = (long) Math.pow(2, retry - 1);
                if (retry == 1) delayMinutes = 0; // 首次立即再试（半分钟内）
                acc.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
                log.info("[OTA] 自动重连失败 id={}, retry={}, nextRetryAfter={}min",
                        acc.getId(), retry, delayMinutes);
            }
            otaAccountMapper.updateById(acc);
            return false;
        }
    }

    /**
     * 安排自动重连（首次触发）
     */
    private void scheduleAutoReconnect(OtaAccount acc) {
        acc.setRetryCount(0);
        // 掉线后 30 秒内开始首次重试
        acc.setNextRetryAt(LocalDateTime.now().plusSeconds(20));
    }

    // ==================== FR-07：风控配置 ====================

    /**
     * 生成/更新风控策略配置
     */
    public OtaAccount updateServerConfig(Long id, String configJson) {
        OtaAccount acc = otaAccountMapper.selectById(id);
        if (acc == null) throw new BusinessException("账号不存在");
        acc.setServerConfig(configJson);
        acc.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.updateById(acc);
        return acc;
    }

    /**
     * 获取默认风控配置（UA、设备指纹等）
     */
    public String getDefaultServerConfig(String platform) {
        // FR-07：固定UA、模拟正常客户端行为
        return String.format("{\"userAgent\":\"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\",\"deviceFingerprint\":\"pms-%s-%s\"}",
                platform, UUID.randomUUID().toString().substring(0, 8));
    }

    // ==================== FR-09：读取订单（手动同步） ====================

    /**
     * FR-09："读取订单"手动即时同步
     * 房态页触发，≤30 秒拉取最新订单更新房态
     */
    public OtaAccount fetchOrders(Long id) {
        OtaAccount acc = otaAccountMapper.selectById(id);
        if (acc == null) throw new BusinessException("账号不存在");
        if (!"CONNECTED".equals(acc.getStatus())) {
            throw new BusinessException("账号未登录，请先登录");
        }
        try {
            // TODO: 实际调用渠道拉订单接口
            doFetchOrdersRequest(acc);
            acc.setLastSyncAt(LocalDateTime.now());
            acc.setLastError(null);
            otaAccountMapper.updateById(acc);

            log.info("[OTA] 读取订单完成 id={}", id);
            return acc;
        } catch (Exception e) {
            acc.setLastError("拉取订单失败: " + e.getMessage());
            otaAccountMapper.updateById(acc);
            throw new BusinessException("拉取订单失败: " + e.getMessage());
        }
    }

    /**
     * FR-02：渠道房源关联后拉取全量同步（占位）
     */
    public OtaAccount fullSync(Long id) {
        OtaAccount acc = otaAccountMapper.selectById(id);
        if (acc == null) throw new BusinessException("账号不存在");
        try {
            doFullSyncRequest(acc);
            acc.setLastSyncAt(LocalDateTime.now());
            acc.setLastError(null);
            acc.setUpdatedAt(LocalDateTime.now());
            otaAccountMapper.updateById(acc);
            return acc;
        } catch (Exception e) {
            acc.setLastError("全量同步失败: " + e.getMessage());
            otaAccountMapper.updateById(acc);
            throw new BusinessException("全量同步失败: " + e.getMessage());
        }
    }

    // ==================== 内部回调调度接口（供 KeepaliveScheduler 使用） ====================

    /**
     * 查询所有需要保活的账号（已登录 + 启用 + 自动重连）
     */
    public List<OtaAccount> findAliveAccounts() {
        return otaAccountMapper.selectList(new LambdaQueryWrapper<OtaAccount>()
                .eq(OtaAccount::getHotelId, hotelId)
                .eq(OtaAccount::getStatus, "CONNECTED")
                .eq(OtaAccount::getEnabled, 1));
    }

    /**
     * 查询所有需要重连的账号（掉线 + 开启自动重连 + 达到重试时间）
     */
    public List<OtaAccount> findReconnectAccounts() {
        return otaAccountMapper.selectList(new LambdaQueryWrapper<OtaAccount>()
                .eq(OtaAccount::getHotelId, hotelId)
                .eq(OtaAccount::getStatus, "OFFLINE")
                .eq(OtaAccount::getAutoReconnect, 1)
                .eq(OtaAccount::getEnabled, 1)
                .isNotNull(OtaAccount::getNextRetryAt)
                .le(OtaAccount::getNextRetryAt, LocalDateTime.now()));
    }

    /**
     * 更新水位线（由 PollingScheduler 调用）
     */
    public void updateAccountWatermark(OtaAccount acc) {
        acc.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.updateById(acc);
    }

    // ==================== 占位实现：真实对接时替换 ====================

    /**
     * 调用渠道登录接口，返回 session token
     */
    private String doChannelLogin(OtaAccount acc) {
        // TODO: 按 platform 分发到各渠道登录实现
        // CTRIP → 携程 EBK 登录接口
        // MEITUAN → 美团商家中心登录
        // DOUYIN → 抖音来客登录
        return "mock_session_token_" + UUID.randomUUID().toString().substring(0, 16);
    }

    private void doKeepaliveRequest(String platform, String token) {
        // TODO: 调用渠道轻量保活接口
    }

    private void doFetchOrdersRequest(OtaAccount acc) {
        // TODO: 拉取最近的订单
    }

    private void doFullSyncRequest(OtaAccount acc) {
        // TODO: 全量同步订单+房态+房价
    }

    private void triggerFullSync(OtaAccount acc) {
        log.info("[OTA] 重连后全量同步触发 id={}", acc.getId());
        doFullSyncRequest(acc);
        acc.setLastSyncAt(LocalDateTime.now());
        otaAccountMapper.updateById(acc);
    }

    private void notifyOffline(OtaAccount acc) {
        // TODO: 站内通知 + 微信通知房东
        log.warn("[OTA] 通知: 账号 {} ({}) 已掉线", acc.getAccountName(), platformName(acc.getPlatform()));
    }

    // ==================== 加密工具 ====================

    /**
     * FR-03：凭证加密存储（AES-128 对称加密，密钥为 32 hex 字符 = 16 字节）
     */
    private String encryptToken(String raw) {
        byte[] key = HexUtil.decodeHex(AES_KEY_HEX);
        AES aes = SecureUtil.aes(key);
        return aes.encryptBase64(raw);
    }

    private String decryptToken(String encrypted) {
        byte[] key = HexUtil.decodeHex(AES_KEY_HEX);
        AES aes = SecureUtil.aes(key);
        return aes.decryptStr(encrypted);
    }

    // ==================== 审计日志 ====================

    private void auditLog(String action, Long targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setStaffId(currentOperator());
        log.setModule("OTA");
        log.setAction(action);
        log.setTargetType("OtaAccount");
        log.setTargetId(targetId);
        log.setDetail("{\"msg\":\"" + (detail != null && detail.length() > 200 ? detail.substring(0, 200) : detail) + "\"}");
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    // ==================== 工具方法 ====================

    private void validate(OtaAccount acc) {
        if (acc.getPlatform() == null || acc.getPlatform().isEmpty()) {
            throw new BusinessException("平台不能为空");
        }
        if (acc.getAccountName() == null || acc.getAccountName().isEmpty()) {
            throw new BusinessException("账号不能为空");
        }
        if (acc.getPassword() == null || acc.getPassword().isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
    }

    private String platformName(String p) {
        switch (p) {
            case "CTRIP": return "携程";
            case "MEITUAN": return "美团";
            case "DOUYIN": return "抖音";
            default: return p;
        }
    }

    private Long currentOperator() {
        StaffContext.StaffPrincipal p = StaffContext.get();
        return p != null ? p.getStaffId() : null;
    }
}
