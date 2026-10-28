package com.pms.module.housekeeping;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
