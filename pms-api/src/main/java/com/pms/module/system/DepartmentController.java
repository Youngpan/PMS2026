package com.pms.module.system;

import com.pms.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public Result<List<Department>> list() {
        return Result.ok(departmentService.list());
    }

    @PostMapping
    public Result<Department> create(@RequestBody Department d) {
        return Result.ok(departmentService.create(d));
    }

    @PutMapping("/{id}")
    public Result<Department> update(@PathVariable Long id, @RequestBody Department d) {
        d.setId(id);
        return Result.ok(departmentService.update(d));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.ok();
    }
}
