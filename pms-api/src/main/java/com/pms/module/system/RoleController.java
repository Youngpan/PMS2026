package com.pms.module.system;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public Result<List<Role>> list() {
        return Result.ok(roleService.list());
    }

    @PostMapping
    public Result<Role> create(@RequestBody Role r) {
        return Result.ok(roleService.create(r));
    }

    @GetMapping("/permissions")
    public Result<List<Permission>> allPermissions() {
        return Result.ok(roleService.allPermissions());
    }

    @GetMapping("/{id}/permissions")
    public Result<List<Permission>> permissionsOfRole(@PathVariable Long id) {
        return Result.ok(roleService.permissionsOfRole(id));
    }

    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("permission_ids");
        List<Long> pids = ids == null ? null : ids.stream().map(Number::longValue).collect(Collectors.toList());
        roleService.assignPermissions(id, pids);
        return Result.ok();
    }
}
