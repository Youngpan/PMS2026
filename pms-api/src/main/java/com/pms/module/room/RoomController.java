package com.pms.module.room;

import com.pms.common.PageResult;
import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public Result<PageResult<Room>> page(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "20") long page_size,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Long room_type_id) {
        return Result.ok(roomService.page(page, page_size, status, room_type_id));
    }

    @PostMapping
    public Result<Room> create(@RequestBody Room room) {
        return Result.ok(roomService.create(room));
    }

    @PutMapping("/{id}/status")
    public Result<Room> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(roomService.changeStatus(id, body.get("status")));
    }

    @GetMapping("/vacant")
    public Result<List<Room>> vacant(@RequestParam(required = false) Long room_type_id) {
        return Result.ok(roomService.vacantRooms(room_type_id));
    }
}
