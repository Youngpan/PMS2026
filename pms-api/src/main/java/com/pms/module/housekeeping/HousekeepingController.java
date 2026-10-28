package com.pms.module.housekeeping;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/housekeeping")
@RequiredArgsConstructor
public class HousekeepingController {

    private final HousekeepingService housekeepingService;

    @GetMapping("/dashboard")
    public Result<List<HousekeepingService.FloorBoard>> dashboard() {
        return Result.ok(housekeepingService.dashboard());
    }

    /** 房态日历：列=房间，首行=日期，格子=订单/在住占用。默认昨天起 20 天。 */
    @GetMapping("/calendar")
    public Result<HousekeepingService.CalendarBoard> calendar(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        LocalDate f = from == null ? LocalDate.now().minusDays(1) : LocalDate.parse(from);
        LocalDate t = to == null ? LocalDate.now().plusDays(20) : LocalDate.parse(to);
        return Result.ok(housekeepingService.calendar(f, t));
    }

    @GetMapping("/tasks")
    public Result<List<HousekeepingTask>> tasks(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long assignee_id) {
        return Result.ok(housekeepingService.tasks(status, assignee_id));
    }

    @PostMapping("/tasks")
    public Result<HousekeepingTask> createTask(@RequestBody Map<String, Object> body) {
        Long roomId = ((Number) body.get("room_id")).longValue();
        String type = (String) body.get("type");
        Long assigneeId = body.get("assignee_id") == null ? null : ((Number) body.get("assignee_id")).longValue();
        return Result.ok(housekeepingService.createTask(roomId, type, assigneeId));
    }

    @PutMapping("/tasks/{id}/finish")
    public Result<HousekeepingTask> finishTask(@PathVariable Long id) {
        return Result.ok(housekeepingService.finishTask(id));
    }

    @GetMapping("/maintenance")
    public Result<List<Maintenance>> maintenance(@RequestParam(required = false) String status) {
        return Result.ok(housekeepingService.maintenances(status));
    }

    @PostMapping("/maintenance")
    public Result<Maintenance> report(@RequestBody Map<String, Object> body) {
        Long roomId = ((Number) body.get("room_id")).longValue();
        String issue = (String) body.get("issue");
        return Result.ok(housekeepingService.reportMaintenance(roomId, issue));
    }

    @PutMapping("/maintenance/{id}/fix")
    public Result<Maintenance> fix(@PathVariable Long id) {
        return Result.ok(housekeepingService.fixMaintenance(id));
    }
}
