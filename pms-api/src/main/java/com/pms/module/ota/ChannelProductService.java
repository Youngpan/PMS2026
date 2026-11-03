package com.pms.module.ota;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.security.StaffContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 渠道房源产品管理服务。
 * 提供产品 CRUD、列表查询、启用/停用等操作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelProductService {

    private final ChannelProductMapper channelProductMapper;
    private final OtaAccountMapper otaAccountMapper;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    public List<ChannelProduct> list(Long otaAccountId, String platform) {
        LambdaQueryWrapper<ChannelProduct> w = new LambdaQueryWrapper<ChannelProduct>()
                .eq(ChannelProduct::getHotelId, hotelId)
                .eq(otaAccountId != null, ChannelProduct::getOtaAccountId, otaAccountId)
                .eq(platform != null && !platform.isEmpty(), ChannelProduct::getPlatform, platform)
                .orderByDesc(ChannelProduct::getCreatedAt);
        return channelProductMapper.selectList(w);
    }

    public ChannelProduct getById(Long id) {
        ChannelProduct p = channelProductMapper.selectById(id);
        if (p == null) throw new BusinessException(404, "产品不存在");
        return p;
    }

    @Transactional
    public ChannelProduct create(ChannelProduct product) {
        // 从 OTA 账号填充平台
        if (product.getOtaAccountId() != null && product.getPlatform() == null) {
            OtaAccount acc = otaAccountMapper.selectById(product.getOtaAccountId());
            if (acc == null) throw new BusinessException(400, "OTA账号不存在");
            product.setPlatform(acc.getPlatform());
        }
        product.setHotelId(hotelId);
        product.setStatus("ACTIVE");
        product.setOperatorId(currentOperator());
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        channelProductMapper.insert(product);
        log.info("[ChannelProduct] 创建产品 id={}, accountId={}, listingId={}",
                product.getId(), product.getOtaAccountId(), product.getChannelListingId());
        return product;
    }

    @Transactional
    public ChannelProduct update(Long id, ChannelProduct product) {
        ChannelProduct exist = channelProductMapper.selectById(id);
        if (exist == null) throw new BusinessException(404, "产品不存在");
        // 更新所有非空字段
        if (product.getChannelListingId() != null) exist.setChannelListingId(product.getChannelListingId());
        if (product.getListingName() != null) exist.setListingName(product.getListingName());
        if (product.getTargetType() != null) exist.setTargetType(product.getTargetType());
        if (product.getRoomId() != null) exist.setRoomId(product.getRoomId());
        if (product.getRoomTypeId() != null) exist.setRoomTypeId(product.getRoomTypeId());
        if (product.getInventoryType() != null) exist.setInventoryType(product.getInventoryType());
        if (product.getProductStatus() != null) exist.setProductStatus(product.getProductStatus());
        if (product.getIsDirect() != null) exist.setIsDirect(product.getIsDirect());
        if (product.getBreakfast() != null) exist.setBreakfast(product.getBreakfast());
        if (product.getBreakfastFee() != null) exist.setBreakfastFee(product.getBreakfastFee());
        if (product.getInvoiceType() != null) exist.setInvoiceType(product.getInvoiceType());
        if (product.getInvoiceTaxRate() != null) exist.setInvoiceTaxRate(product.getInvoiceTaxRate());
        if (product.getCheckinMethod() != null) exist.setCheckinMethod(product.getCheckinMethod());
        if (product.getPaymentMethod() != null) exist.setPaymentMethod(product.getPaymentMethod());
        if (product.getGuaranteePolicy() != null) exist.setGuaranteePolicy(product.getGuaranteePolicy());
        if (product.getCancelPolicy() != null) exist.setCancelPolicy(product.getCancelPolicy());
        if (product.getNs() != null) exist.setNs(product.getNs());
        if (product.getNr() != null) exist.setNr(product.getNr());
        if (product.getCapacity() != null) exist.setCapacity(product.getCapacity());
        if (product.getExtraBed() != null) exist.setExtraBed(product.getExtraBed());
        if (product.getExtraPerson() != null) exist.setExtraPerson(product.getExtraPerson());
        if (product.getChildPolicy() != null) exist.setChildPolicy(product.getChildPolicy());
        if (product.getPetPolicy() != null) exist.setPetPolicy(product.getPetPolicy());
        if (product.getBedType() != null) exist.setBedType(product.getBedType());
        if (product.getFloor() != null) exist.setFloor(product.getFloor());
        if (product.getWindow() != null) exist.setWindow(product.getWindow());
        if (product.getArea() != null) exist.setArea(product.getArea());
        exist.setUpdatedAt(LocalDateTime.now());
        exist.setOperatorId(currentOperator());
        channelProductMapper.updateById(exist);
        log.info("[ChannelProduct] 更新产品 id={}", id);
        return exist;
    }

    @Transactional
    public void toggleStatus(Long id, String productStatus) {
        ChannelProduct exist = channelProductMapper.selectById(id);
        if (exist == null) throw new BusinessException(404, "产品不存在");
        exist.setProductStatus(productStatus);
        exist.setUpdatedAt(LocalDateTime.now());
        channelProductMapper.updateById(exist);
        log.info("[ChannelProduct] 切换产品状态 id={}, status={}", id, productStatus);
    }

    @Transactional
    public void deleteById(Long id) {
        ChannelProduct exist = channelProductMapper.selectById(id);
        if (exist == null) throw new BusinessException(404, "产品不存在");
        channelProductMapper.deleteById(id);
        log.info("[ChannelProduct] 删除产品 id={}", id);
    }

    private Long currentOperator() {
        StaffContext.StaffPrincipal p = StaffContext.get();
        return p != null ? p.getStaffId() : null;
    }
}
