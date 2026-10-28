package com.pms.module.reservation;

import com.pms.common.PageResult;
import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public Result<PageResult<Reservation>> page(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "20") long page_size,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String phone,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.ok(reservationService.page(page, page_size, status, phone, from, to));
    }

    @com.pms.common.OpLog(module = "M3", action = "新建预订")
    @com.pms.common.Idempotent
    @PostMapping
    public Result<ReservationService.Detail> create(@RequestBody ReservationService.CreateReq req) {
        return Result.ok(reservationService.create(req));
    }

    @GetMapping("/{id}")
    public Result<ReservationService.Detail> detail(@PathVariable Long id) {
        return Result.ok(reservationService.detail(id));
    }

    @com.pms.common.OpLog(module = "M3", action = "确认预订")
    @PostMapping("/{id}/confirm")
    public Result<Reservation> confirm(@PathVariable Long id) {
        return Result.ok(reservationService.confirm(id));
    }

    @com.pms.common.OpLog(module = "M3", action = "取消预订")
    @PostMapping("/{id}/cancel")
    public Result<Reservation> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        return Result.ok(reservationService.cancel(id, reason));
    }

    @PostMapping("/rooms/{reservationRoomId}/assign")
    public Result<ReservationRoom> assignRoom(@PathVariable Long reservationRoomId,
                                              @RequestBody Map<String, Object> body) {
        Long roomId = ((Number) body.get("room_id")).longValue();
        return Result.ok(reservationService.assignRoom(reservationRoomId, roomId));
    }
}
