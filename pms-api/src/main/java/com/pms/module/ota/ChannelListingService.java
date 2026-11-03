package com.pms.module.ota;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.module.room.Room;
import com.pms.module.room.RoomMapper;
import com.pms.module.room.RoomType;
import com.pms.module.room.RoomTypeMapper;
import com.pms.security.StaffContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 渠道房源关联与房态同步服务。
 *
 * 核心逻辑：
 * 1. 房源关联：百居易房间/房型 ↔ OTA 渠道房源
 * 2. 关联瞬间库存同步：以百居易本地库存覆盖渠道
 * 3. 来单自动关房（防撞单主路径）
 * 4. 房态变更下行推送
 * 5. 冲突订单盒子
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelListingService {

    private final ChannelListingMapper channelListingMapper;
    private final ConflictOrderMapper conflictOrderMapper;
    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final OtaAccountMapper otaAccountMapper;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    // ==================== 房源关联 CRUD ====================

    /**
     * 查询某 OTA 账号下的所有关联
     */
    public List<ChannelListing> listByAccount(Long otaAccountId) {
        return channelListingMapper.selectList(new LambdaQueryWrapper<ChannelListing>()
                .eq(ChannelListing::getHotelId, hotelId)
                .eq(ChannelListing::getOtaAccountId, otaAccountId)
                .eq(ChannelListing::getStatus, "ACTIVE")
                .orderByDesc(ChannelListing::getCreatedAt));
    }

    /**
     * 查询关联到某房间的所有渠道房源
     */
    public List<ChannelListing> listByRoom(Long roomId) {
        return channelListingMapper.selectList(new LambdaQueryWrapper<ChannelListing>()
                .eq(ChannelListing::getHotelId, hotelId)
                .eq(ChannelListing::getTargetType, "ROOM")
                .eq(ChannelListing::getRoomId, roomId)
                .eq(ChannelListing::getStatus, "ACTIVE"));
    }

    /**
     * 查询关联到某房型的所有渠道房源
     */
    public List<ChannelListing> listByRoomType(Long roomTypeId) {
        return channelListingMapper.selectList(new LambdaQueryWrapper<ChannelListing>()
                .eq(ChannelListing::getHotelId, hotelId)
                .eq(ChannelListing::getTargetType, "ROOM_TYPE")
                .eq(ChannelListing::getRoomTypeId, roomTypeId)
                .eq(ChannelListing::getStatus, "ACTIVE"));
    }

    /**
     * 查询所有已关联的渠道
     */
    public List<ChannelListing> listAll() {
        return channelListingMapper.selectList(new LambdaQueryWrapper<ChannelListing>()
                .eq(ChannelListing::getHotelId, hotelId)
                .eq(ChannelListing::getStatus, "ACTIVE"));
    }

    /**
     * 建立关联（房间→渠道房源 / 房型→渠道房源）
     *
     * 关联瞬间：
     * - 独立房源：关联到房间
     * - 多库存房源：关联到房型
     * - 以百居易本地库存为准，覆盖渠道库存
     */
    @Transactional
    public ChannelListing bind(ChannelListing listing) {
        validate(listing);
        // 从 OTA 账号自动填充平台
        OtaAccount acc = otaAccountMapper.selectById(listing.getOtaAccountId());
        if (acc == null) throw new BusinessException(400, "OTA账号不存在");
        listing.setPlatform(acc.getPlatform());
        listing.setHotelId(hotelId);
        listing.setStatus("ACTIVE");
        listing.setOperatorId(currentOperator());
        listing.setCreatedAt(LocalDateTime.now());
        listing.setUpdatedAt(LocalDateTime.now());

        // 检查同一账号 + 渠道房源ID是否已关联
        ChannelListing exist = channelListingMapper.selectOne(
                new LambdaQueryWrapper<ChannelListing>()
                        .eq(ChannelListing::getOtaAccountId, listing.getOtaAccountId())
                        .eq(ChannelListing::getChannelListingId, listing.getChannelListingId())
                        .eq(ChannelListing::getStatus, "ACTIVE"));
        if (exist != null) {
            throw new BusinessException(400, "该渠道房源已关联");
        }

        channelListingMapper.insert(listing);

        // 关联瞬间：下发本地库存到渠道（占位）
        // TODO: 调用 doSyncInventory(listing)，将本地空房数推送到 OTA
        log.info("[ChannelListing] 房源关联成功 id={}, accountId={}, targetType={}, targetId={}",
                listing.getId(), listing.getOtaAccountId(), listing.getTargetType(),
                listing.getTargetType().equals("ROOM") ? listing.getRoomId() : listing.getRoomTypeId());

        return listing;
    }

    /**
     * 解除关联
     */
    @Transactional
    public void unbind(Long id) {
        ChannelListing listing = channelListingMapper.selectById(id);
        if (listing == null) throw new BusinessException("关联不存在");
        listing.setStatus("INACTIVE");
        listing.setUpdatedAt(LocalDateTime.now());
        channelListingMapper.updateById(listing);
        log.info("[ChannelListing] 房源关联解除 id={}", id);
    }

    // ==================== 房态同步（下行：本地→OTA） ====================

    /**
     * 当本地房态变更时，通知所有关联到该房间/房型的渠道房源。
     * 由外部调用（如 blockRoom/releaseBlock 后）。
     *
     * @param targetType ROOM 或 ROOM_TYPE
     * @param targetId   房间ID 或 房型ID
     * @param date       变更日期
     * @param isBlock    关房(true) 还是 开房(false)
     */
    public void notifyRoomStatusChange(String targetType, Long targetId, LocalDate date, boolean isBlock) {
        List<ChannelListing> listings;
        if ("ROOM".equals(targetType)) {
            listings = listByRoom(targetId);
        } else {
            listings = listByRoomType(targetId);
        }
        if (listings.isEmpty()) return;

        for (ChannelListing cl : listings) {
            try {
                // TODO: 调用 OTA 渠道房态更新接口
                doChannelStatusUpdate(cl, date, isBlock);
                log.debug("[OTA Sync] 房态同步 accountId={}, listingId={}, date={}, isBlock={}",
                        cl.getOtaAccountId(), cl.getChannelListingId(), date, isBlock);
            } catch (Exception e) {
                log.warn("[OTA Sync] 房态同步失败 accountId={}, listingId={}, err={}",
                        cl.getOtaAccountId(), cl.getChannelListingId(), e.getMessage());
            }
        }
    }

    /**
     * 来单自动关房（防撞单主路径）
     * 当某个渠道来单 → 自动关闭关联到同一房间/房型的其他渠道房态
     *
     * @param roomId     订单占用的房间ID
     * @param roomTypeId 订单占用的房型ID（多库存场景）
     * @param start      入住日期
     * @param end        离店日期
     * @param sourceOtaAccountId 来源OTA账号ID（不关此账号）
     */
    public void autoCloseOtherChannels(Long roomId, Long roomTypeId, LocalDate start, LocalDate end,
                                       Long sourceOtaAccountId) {
        // 找到需要关房的日期列表
        List<LocalDate> dates = start.datesUntil(end).toList();

        if (roomId != null) {
            // 独立房源：查询所有关联到该房间的渠道房源（排除来源渠道）
            List<ChannelListing> roomListings = listByRoom(roomId);
            for (ChannelListing cl : roomListings) {
                if (cl.getOtaAccountId().equals(sourceOtaAccountId)) continue;
                for (LocalDate d : dates) {
                    try {
                        doChannelStatusUpdate(cl, d, true); // 关房
                    } catch (Exception e) {
                        log.warn("[OTA 防撞单] 关房失败 accountId={}, listingId={}, date={}, err={}",
                                cl.getOtaAccountId(), cl.getChannelListingId(), d, e.getMessage());
                    }
                }
            }
        }

        if (roomTypeId != null) {
            // 多库存房源：查询所有关联到该房型的渠道房源
            List<ChannelListing> typeListings = listByRoomType(roomTypeId);
            for (ChannelListing cl : typeListings) {
                if (cl.getOtaAccountId().equals(sourceOtaAccountId)) continue;
                for (LocalDate d : dates) {
                    try {
                        doChannelStatusUpdate(cl, d, true);
                    } catch (Exception e) {
                        log.warn("[OTA 防撞单] 关房失败 accountId={}, listingId={}, date={}",
                                cl.getOtaAccountId(), cl.getChannelListingId(), d);
                    }
                }
            }
        }
    }

    // ==================== 冲突订单盒子 ====================

    /**
     * 添加冲突订单到订单盒子
     */
    public ConflictOrder addConflictOrder(ConflictOrder order) {
        order.setHotelId(hotelId);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        conflictOrderMapper.insert(order);
        log.warn("[ConflictOrder] 冲突订单入盒 id={}, type={}, detail={}",
                order.getId(), order.getConflictType(), order.getConflictDetail());
        return order;
    }

    /**
     * 查询待处理的冲突订单
     */
    public List<ConflictOrder> listPendingConflicts() {
        return conflictOrderMapper.selectList(new LambdaQueryWrapper<ConflictOrder>()
                .eq(ConflictOrder::getHotelId, hotelId)
                .eq(ConflictOrder::getStatus, "PENDING")
                .orderByDesc(ConflictOrder::getCreatedAt));
    }

    /**
     * 处理冲突订单
     */
    public ConflictOrder resolveConflict(Long id, String resolution, String newStatus) {
        ConflictOrder order = conflictOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("冲突订单不存在");
        order.setStatus(newStatus != null ? newStatus : "RESOLVED");
        order.setResolution(resolution);
        order.setHandlerId(currentOperator());
        order.setHandledAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        conflictOrderMapper.updateById(order);
        return order;
    }

    // ==================== 下行同步占位（真实对接时替换） ====================

    private void doChannelStatusUpdate(ChannelListing listing, LocalDate date, boolean isBlock) {
        // TODO: 调用各 OTA 平台房态更新接口
        // CTRIP → 携程 EBK 房态接口
        // MEITUAN → 美团房价房态接口
        // DOUYIN → 抖音来客房态接口
    }

    // ==================== 工具 ====================

    private void validate(ChannelListing listing) {
        if (listing.getOtaAccountId() == null) throw new BusinessException("OTA账号不能为空");
        if (listing.getChannelListingId() == null || listing.getChannelListingId().isEmpty())
            throw new BusinessException("渠道房源ID不能为空");
        if (!"ROOM".equals(listing.getTargetType()) && !"ROOM_TYPE".equals(listing.getTargetType()))
            throw new BusinessException("关联类型必须是 ROOM 或 ROOM_TYPE");
        if ("ROOM".equals(listing.getTargetType()) && listing.getRoomId() == null)
            throw new BusinessException("关联到房间时，房间ID不能为空");
        if ("ROOM_TYPE".equals(listing.getTargetType()) && listing.getRoomTypeId() == null)
            throw new BusinessException("关联到房型时，房型ID不能为空");
    }

    private Long currentOperator() {
        StaffContext.StaffPrincipal p = StaffContext.get();
        return p != null ? p.getStaffId() : null;
    }
}
