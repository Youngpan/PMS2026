package com.pms.module.system;

import com.pms.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 全局参数：服务费率、默认押金、退房时间等（存于 hotel 表）。
 */
@Service
@RequiredArgsConstructor
public class ConfigService {

    private final HotelMapper hotelMapper;

    @org.springframework.beans.factory.annotation.Value("${pms.hotel.id}")
    private Long hotelId;

    public Hotel get() {
        Hotel h = hotelMapper.selectById(hotelId);
        if (h == null) throw new BusinessException(404, "酒店配置不存在");
        return h;
    }

    public Hotel update(Hotel patch) {
        Hotel h = get();
        if (patch.getName() != null) h.setName(patch.getName());
        if (patch.getAddress() != null) h.setAddress(patch.getAddress());
        if (patch.getPhone() != null) h.setPhone(patch.getPhone());
        if (patch.getServiceFeeRate() != null) h.setServiceFeeRate(patch.getServiceFeeRate());
        if (patch.getDefaultDeposit() != null) h.setDefaultDeposit(patch.getDefaultDeposit());
        if (patch.getCheckoutTime() != null) h.setCheckoutTime(patch.getCheckoutTime());
        hotelMapper.updateById(h);
        return h;
    }
}
