package com.pms.module.room;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

/**
 * 批量新增房号请求。
 *
 * @param roomTypeId 房型 ID
 * @param floor      楼层（可空，空则从房号首位推断）
 * @param numbers    房号列表，如 ["301", "302"]
 * @param remark     备注（可空）
 */
public record RoomBatchRequest(
        @JsonAlias("room_type_id") Long roomTypeId,
        Integer floor,
        List<String> numbers,
        String remark) {
}
