package com.pms.module.folio;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import com.pms.module.stay.Stay;
import com.pms.security.StaffContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单/账务明细/收款/转账/分账。
 * 余额 = Σ folio_item.amount（消费为正，押金/支付为负）；余额 ≤ 0 时关账。
 */
@Service
@RequiredArgsConstructor
public class FolioService {

    private final FolioMapper folioMapper;
    private final FolioItemMapper folioItemMapper;
    private final PaymentMapper paymentMapper;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    /** 确保在住有对应账单（幂等：按 stay_id 复用）。 */
    @Transactional
    public Folio ensureFolio(Stay stay) {
        Folio exist = folioMapper.selectOne(new LambdaQueryWrapper<Folio>()
                .eq(Folio::getStayId, stay.getId())
                .eq(Folio::getStatus, "OPEN")
                .last("LIMIT 1"));
        if (exist != null) return exist;

        Folio f = new Folio();
        f.setHotelId(hotelId);
        f.setStayId(stay.getId());
        f.setReservationId(stay.getReservationId());
        f.setGuestId(stay.getGuestId());
        f.setBalance(BigDecimal.ZERO);
        f.setStatus("OPEN");
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());
        folioMapper.insert(f);
        return f;
    }

    public Folio get(Long id) {
        Folio f = folioMapper.selectById(id);
        if (f == null) throw new BusinessException(404, "账单不存在");
        return f;
    }

    public FolioDetail detail(Long id) {
        Folio f = get(id);
        List<FolioItem> items = folioItemMapper.selectList(new LambdaQueryWrapper<FolioItem>()
                .eq(FolioItem::getFolioId, id)
                .orderByAsc(FolioItem::getCreatedAt));
        FolioDetail d = new FolioDetail();
        d.folio = f;
        d.items = items;
        return d;
    }

    public Folio getByStayId(Long stayId) {
        return folioMapper.selectOne(new LambdaQueryWrapper<Folio>()
                .eq(Folio::getStayId, stayId)
                .eq(Folio::getStatus, "OPEN")
                .last("LIMIT 1"));
    }

    /** 录消费（房费/杂费）。amount > 0。 */
    @Transactional
    public FolioItem addCharge(Long folioId, String category, BigDecimal amount, String remark) {
        FolioItem item = addItem(folioId, "CHARGE", category, amount.abs(), remark, null);
        recompute(folioId);
        return item;
    }

    /** 录押金：明细为负，同时记 payment。 */
    @Transactional
    public FolioItem addDeposit(Long folioId, BigDecimal amount, String method) {
        FolioItem item = addItem(folioId, "DEPOSIT", "押金", amount.abs().negate(), "押金", null);
        recordPayment(folioId, method, amount.abs());
        recompute(folioId);
        return item;
    }

    /** 收款：明细为负，记 payment，余额 ≤ 0 则关账。 */
    @Transactional
    public Folio pay(Long folioId, String method, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "收款金额必须为正");
        }
        addItem(folioId, "PAYMENT", "收款", amount.negate(), method + " 收款", null);
        recordPayment(folioId, method, amount);
        Folio f = recompute(folioId);
        // 仅在收款后判断关账：押金/录消费不应触发关账
        if (f.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            f.setStatus("CLOSED");
            f.setUpdatedAt(LocalDateTime.now());
            folioMapper.updateById(f);
        }
        return f;
    }

    /** 转账：将指定明细转入目标账单（双方一正一负）。 */
    @Transactional
    public void transfer(Long fromFolioId, Long toFolioId, List<Long> itemIds) {
        get(fromFolioId);
        get(toFolioId);
        for (Long itemId : itemIds) {
            FolioItem src = folioItemMapper.selectById(itemId);
            if (src == null || !src.getFolioId().equals(fromFolioId)) {
                throw new BusinessException(400, "明细不属于该账单: " + itemId);
            }
            BigDecimal amt = src.getAmount();
            // 目标账单加正向
            addItem(toFolioId, src.getType(), src.getCategory(), amt, "转账自 #" + fromFolioId, fromFolioId);
            // 源账单加反向
            addItem(fromFolioId, src.getType(), src.getCategory(), amt.negate(), "转出至 #" + toFolioId, toFolioId);
        }
        recompute(fromFolioId);
        recompute(toFolioId);
    }

    /** 分账：将某明细部分金额拆到目标账单。 */
    @Transactional
    public void split(Long fromFolioId, List<SplitItem> items) {
        get(fromFolioId);
        for (SplitItem s : items) {
            FolioItem src = folioItemMapper.selectById(s.itemId);
            if (src == null || !src.getFolioId().equals(fromFolioId)) {
                throw new BusinessException(400, "明细不属于该账单: " + s.itemId);
            }
            get(s.toFolioId);
            BigDecimal amt = s.amount.abs();
            addItem(s.toFolioId, src.getType(), src.getCategory(), amt, "分账自 #" + fromFolioId, fromFolioId);
            addItem(fromFolioId, src.getType(), src.getCategory(), amt.negate(), "分账至 #" + s.toFolioId, s.toFolioId);
        }
        recompute(fromFolioId);
        items.forEach(s -> recompute(s.toFolioId));
    }

    /** 退房结算：收款后调用，余额 ≤ 0 关账。返回更新后账单。 */
    @Transactional
    public Folio settle(Long folioId, String method, BigDecimal amount) {
        return pay(folioId, method, amount);
    }

    // ---------- 内部 ----------

    private FolioItem addItem(Long folioId, String type, String category, BigDecimal amount, String remark, Long refFolioId) {
        FolioItem item = new FolioItem();
        item.setFolioId(folioId);
        item.setType(type);
        item.setCategory(category);
        item.setAmount(amount);
        item.setRefFolioId(refFolioId);
        item.setRemark(remark);
        item.setOperatorId(currentOperator());
        item.setCreatedAt(LocalDateTime.now());
        folioItemMapper.insert(item);
        return item;
    }

    private void recordPayment(Long folioId, String method, BigDecimal amount) {
        Payment p = new Payment();
        p.setFolioId(folioId);
        p.setMethod(method);
        p.setAmount(amount);
        p.setOperatorId(currentOperator());
        p.setPaidAt(LocalDateTime.now());
        paymentMapper.insert(p);
    }

    /** 仅重算余额（不改变 status）；关账由 pay/settle 显式判断。 */
    private Folio recompute(Long folioId) {
        Folio f = get(folioId);
        List<FolioItem> items = folioItemMapper.selectList(new LambdaQueryWrapper<FolioItem>()
                .eq(FolioItem::getFolioId, folioId));
        BigDecimal bal = items.stream()
                .map(FolioItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        f.setBalance(bal);
        f.setUpdatedAt(LocalDateTime.now());
        folioMapper.updateById(f);
        return f;
    }

    private long currentOperator() {
        return StaffContext.get() != null ? StaffContext.get().getStaffId() : 0L;
    }

    // ---------- DTO ----------

    @lombok.Data
    public static class FolioDetail {
        public Folio folio;
        public List<FolioItem> items;
    }

    @lombok.Data
    public static class SplitItem {
        public Long itemId;
        public Long toFolioId;
        public BigDecimal amount;
    }
}
