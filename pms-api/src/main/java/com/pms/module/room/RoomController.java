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
                                        @RequestParam(required = false) String rent_status,
                                        @RequestParam(required = false) String clean_status,
                                        @RequestParam(required = false) Long room_type_id) {
        return Result.ok(roomService.page(page, page_size, rent_status, clean_status, room_type_id));
    }

    @PostMapping
    public Result<Room> create(@RequestBody Room room) {
        return Result.ok(roomService.create(room));
    }

    /** 批量新增房号（一次录入整层/整栋）。 */
    @PostMapping("/batch")
    public Result<List<Room>> batchCreate(@RequestBody RoomBatchRequest req) {
        return Result.ok(roomService.batchCreate(req));
    }

    /** 修改房号基础信息：房号 / 楼层 / 房型 / 备注。 */
    @PutMapping("/{id}")
    public Result<Room> update(@PathVariable Long id, @RequestBody Room room) {
        return Result.ok(roomService.update(id, room));
    }

    /** 切换可售状态：OPEN 打开 / CLOSED 关闭。 */
    @PutMapping("/{id}/rent-status")
    public Result<Room> changeRentStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(roomService.setRentStatus(id, body.get("rent_status")));
    }

    /** 切换卫生状态：CLEAN 净 / DIRTY 脏。 */
    @PutMapping("/{id}/clean-status")
    public Result<Room> changeCleanStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(roomService.setCleanStatus(id, body.get("clean_status")));
    }

    /** 更新房间备注。 */
    @PutMapping("/{id}/remark")
    public Result<Room> changeRemark(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(roomService.setRemark(id, body.get("remark")));
    }

    @GetMapping("/vacant")
    public Result<List<Room>> vacant(@RequestParam(required = false) Long room_type_id) {
        return Result.ok(roomService.vacantRooms(room_type_id));
    }
}
