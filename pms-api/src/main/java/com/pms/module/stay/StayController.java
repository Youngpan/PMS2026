package com.pms.module.stay;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stays")
@RequiredArgsConstructor
public class StayController {

    private final StayService stayService;

    @com.pms.common.OpLog(module = "M2", action = "入住")
    @com.pms.common.Idempotent
    @PostMapping("/checkins")
    public Result<List<Stay>> checkin(@RequestBody StayService.CheckinReq req) {
        return Result.ok(stayService.checkin(req));
    }

    @GetMapping
    public Result<List<Stay>> inHouse() {
        return Result.ok(stayService.inHouse());
    }

    @GetMapping("/{id}")
    public Result<Stay> get(@PathVariable Long id) {
        return Result.ok(stayService.get(id));
    }

    @PostMapping("/{id}/extend")
    public Result<StayExtension> extend(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(stayService.extend(id, LocalDate.parse(body.get("extend_to_date"))));
    }

    @PostMapping("/{id}/transfer-room")
    public Result<Stay> transfer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long toRoomId = ((Number) body.get("to_room_id")).longValue();
        String reason = (String) body.get("reason");
        return Result.ok(stayService.transferRoom(id, toRoomId, reason));
    }

    @PutMapping("/{id}/rate")
    public Result<Stay> rate(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return Result.ok(stayService.updateRate(id, new java.math.BigDecimal(body.get("rate").toString())));
    }

    @com.pms.common.OpLog(module = "M2", action = "退房")
    @com.pms.common.Idempotent
    @PostMapping("/{id}/checkout")
    public Result<Stay> checkout(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        String method = body == null ? "CASH" : (String) body.getOrDefault("payment_method", "CASH");
        java.math.BigDecimal amount = null;
        if (body != null && body.get("amount") != null) {
            amount = new java.math.BigDecimal(body.get("amount").toString());
        }
        return Result.ok(stayService.checkout(id, method, amount));
    }
}
