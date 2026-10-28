package com.pms.module.system;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public Result<List<Staff>> list() {
        return Result.ok(staffService.list());
    }

    /** 创建员工，body 含 plain_password。 */
    @PostMapping
    public Result<Staff> create(@RequestBody Map<String, Object> body) {
        Staff s = new Staff();
        s.setUsername((String) body.get("username"));
        s.setRealName((String) body.get("real_name"));
        s.setPhone((String) body.get("phone"));
        s.setDepartmentId(body.get("department_id") == null ? null : ((Number) body.get("department_id")).longValue());
        String pwd = (String) body.get("plain_password");
        return Result.ok(staffService.create(s, pwd));
    }

    @PutMapping("/{id}")
    public Result<Staff> update(@PathVariable Long id, @RequestBody Staff s) {
        s.setId(id);
        return Result.ok(staffService.update(s));
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        staffService.resetPassword(id, body.get("new_password"));
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        staffService.toggleStatus(id, ((Number) body.get("status")).intValue());
        return Result.ok();
    }

    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("role_ids");
        List<Long> roleIds = ids == null ? null : ids.stream().map(Number::longValue).toList();
        staffService.assignRoles(id, roleIds);
        return Result.ok();
    }
}
