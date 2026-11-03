package com.pms.module.ota;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.security.StaffContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OTA 账号管理：增删改查、测试连接、手动同步。
 * 当前为框架实现：测试连接和同步仅校验账号非空并标记状态，
 * 真实 API 对接在各平台开放授权后填充 doConnect/doSync。
 */
@Service
@RequiredArgsConstructor
public class OtaAccountService {

    private final OtaAccountMapper otaAccountMapper;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    public List<OtaAccount> list(String platform) {
        LambdaQueryWrapper<OtaAccount> w = new LambdaQueryWrapper<OtaAccount>()
                .eq(OtaAccount::getHotelId, hotelId)
                .eq(platform != null && !platform.isEmpty(), OtaAccount::getPlatform, platform)
                .orderByDesc(OtaAccount::getCreatedAt);
        return otaAccountMapper.selectList(w);
    }

    public OtaAccount create(OtaAccount acc) {
        validate(acc);
        acc.setHotelId(hotelId);
        acc.setStatus("DISCONNECTED");
        acc.setEnabled(1);
        acc.setOperatorId(currentOperator());
        acc.setCreatedAt(LocalDateTime.now());
        acc.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.insert(acc);
        return acc;
    }

    public OtaAccount update(Long id, OtaAccount acc) {
        OtaAccount exist = otaAccountMapper.selectById(id);
        if (exist == null) throw new BusinessException("账号不存在");
        exist.setPlatform(acc.getPlatform());
        exist.setAccountName(acc.getAccountName());
        exist.setMerchantId(acc.getMerchantId());
        exist.setAppKey(acc.getAppKey());
        // 密码/AppSecret 仅在前端传了新值时更新
        if (acc.getPassword() != null && !acc.getPassword().isEmpty()) {
            exist.setPassword(acc.getPassword());
        }
        if (acc.getAppSecret() != null && !acc.getAppSecret().isEmpty()) {
            exist.setAppSecret(acc.getAppSecret());
        }
        exist.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.updateById(exist);
        return exist;
    }

    public void delete(Long id) {
        otaAccountMapper.deleteById(id);
    }

    public OtaAccount toggleEnabled(Long id, Integer enabled) {
        OtaAccount acc = otaAccountMapper.selectById(id);
        if (acc == null) throw new BusinessException("账号不存在");
        acc.setEnabled(enabled);
        acc.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.updateById(acc);
        return acc;
    }

    /**
     * 测试连接：框架实现——校验账号密码非空后标记 CONNECTED。
     * 真实对接时替换为调用各 OTA 平台登录接口。
     */
    public OtaAccount testConnection(Long id) {
        OtaAccount acc = otaAccountMapper.selectById(id);
        if (acc == null) throw new BusinessException("账号不存在");
        if (acc.getAccountName() == null || acc.getAccountName().isEmpty()) {
            throw new BusinessException("账号名不能为空");
        }
        try {
            // TODO: 调用真实 OTA 平台登录接口
            // boolean ok = doConnect(acc.getPlatform(), acc.getAccountName(), acc.getPassword(), acc.getMerchantId());
            doConnect(acc);
            acc.setStatus("CONNECTED");
            acc.setLastError(null);
            acc.setLastSyncAt(LocalDateTime.now());
        } catch (Exception e) {
            acc.setStatus("ERROR");
            acc.setLastError(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 490)) : "连接失败");
        }
        acc.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.updateById(acc);
        return acc;
    }

    /**
     * 手动同步：框架实现——更新最近同步时间。
     * 真实对接时替换为拉取各 OTA 平台订单并写入 reservation。
     */
    public OtaAccount sync(Long id) {
        OtaAccount acc = otaAccountMapper.selectById(id);
        if (acc == null) throw new BusinessException("账号不存在");
        if (!"CONNECTED".equals(acc.getStatus())) {
            throw new BusinessException("账号未连接，请先测试连接");
        }
        try {
            // TODO: 调用真实 OTA 平台订单拉取接口
            // List<OtaOrder> orders = doSync(acc);
            doSync(acc);
            acc.setLastSyncAt(LocalDateTime.now());
            acc.setLastError(null);
        } catch (Exception e) {
            acc.setStatus("ERROR");
            acc.setLastError(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 490)) : "同步失败");
        }
        acc.setUpdatedAt(LocalDateTime.now());
        otaAccountMapper.updateById(acc);
        return acc;
    }

    // ---- 框架占位：真实对接时替换 ----
    private void doConnect(OtaAccount acc) {
        // 携程 EBK / 美团商家 / 抖音来客 登录逻辑占位
        // 当前仅模拟：账号密码非空即视为连接成功
    }

    private void doSync(OtaAccount acc) {
        // 拉取 OTA 订单并写入 reservation 表的逻辑占位
    }

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

    private Long currentOperator() {
        StaffContext.StaffPrincipal p = StaffContext.get();
        return p != null ? p.getStaffId() : null;
    }
}
