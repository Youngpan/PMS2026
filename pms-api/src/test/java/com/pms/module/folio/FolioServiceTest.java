package com.pms.module.folio;

import com.pms.module.stay.Stay;
import com.pms.module.stay.StayMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 账单余额与关账逻辑测试。
 * 重点回归：押金（负明细）不应触发关账 —— 该 bug 曾导致 getByStayId 返回 null、控制器 NPE。
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class FolioServiceTest {

    @Autowired FolioService folioService;
    @Autowired FolioMapper folioMapper;
    @Autowired StayMapper stayMapper;

    private Folio newFolioForFreshStay() {
        Stay s = new Stay();
        s.setHotelId(1L);
        s.setReservationId(null);
        s.setGuestId(null);
        s.setRoomId(1L);
        s.setRoomTypeId(1L);
        s.setRate(new BigDecimal("280"));
        s.setCheckInAt(LocalDateTime.now());
        s.setStatus("IN");
        s.setOperatorId(0L);
        stayMapper.insert(s);
        return folioService.ensureFolio(s);
    }

    @Test
    void 押金使余额为负但不应关账() {
        Folio f = newFolioForFreshStay();

        folioService.addDeposit(f.getId(), new BigDecimal("500"), "CASH");

        Folio after = folioMapper.selectById(f.getId());
        assertThat(after.getBalance()).isEqualByComparingTo(new BigDecimal("-500"));
        assertThat(after.getStatus()).as("押金不应关账（回归保护）").isEqualTo("OPEN");

        // 这正是 smoke step 8 走的路径：按 stay 查 OPEN 账单，押金后必须仍可查到
        Folio byStay = folioService.getByStayId(after.getStayId());
        assertThat(byStay).as("getByStayId 在押金后必须返回账单").isNotNull();
        assertThat(byStay.getId()).isEqualTo(after.getId());
    }

    @Test
    void 录消费更新余额且不关账() {
        Folio f = newFolioForFreshStay();
        folioService.addDeposit(f.getId(), new BigDecimal("500"), "CASH"); // -500
        folioService.addCharge(f.getId(), "room-charge", new BigDecimal("280"), "房费"); // +280

        Folio after = folioMapper.selectById(f.getId());
        assertThat(after.getBalance()).isEqualByComparingTo(new BigDecimal("-220"));
        assertThat(after.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void 收款清零后关账() {
        Folio f = newFolioForFreshStay();
        folioService.addCharge(f.getId(), "room-charge", new BigDecimal("280"), "房费"); // +280
        // 收 280，余额归零 → 关账
        Folio after = folioService.pay(f.getId(), "WECHAT", new BigDecimal("280"));

        assertThat(after.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(after.getStatus()).as("余额清零应关账").isEqualTo("CLOSED");
    }

    @Test
    void 多收结余也关账() {
        Folio f = newFolioForFreshStay();
        folioService.addCharge(f.getId(), "mini-bar", new BigDecimal("100"), " minibar"); // +100
        Folio after = folioService.pay(f.getId(), "CASH", new BigDecimal("150")); // -150 → -50

        assertThat(after.getBalance()).isEqualByComparingTo(new BigDecimal("-50"));
        assertThat(after.getStatus()).isEqualTo("CLOSED");
    }

    @Test
    void detail返回全部明细() {
        Folio f = newFolioForFreshStay();
        folioService.addDeposit(f.getId(), new BigDecimal("500"), "CASH");
        folioService.addCharge(f.getId(), "room-charge", new BigDecimal("280"), "房费");

        FolioService.FolioDetail d = folioService.detail(f.getId());
        assertThat(d.folio.getId()).isEqualTo(f.getId());
        assertThat(d.items).hasSize(2);
    }

    @Test
    void 收款金额非正应拒绝() {
        Folio f = newFolioForFreshStay();
        try {
            folioService.pay(f.getId(), "CASH", BigDecimal.ZERO);
            assertThat(false).as("0 元收款应抛异常").isTrue();
        } catch (com.pms.common.BusinessException expected) {
            // ok
        }
    }
}
