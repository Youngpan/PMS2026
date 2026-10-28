package com.pms.module.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;

    @Value("${pms.hotel.id}")
    private Long hotelId;

    public List<Department> list() {
        return departmentMapper.selectList(new LambdaQueryWrapper<Department>()
                .eq(Department::getHotelId, hotelId)
                .orderByAsc(Department::getCode));
    }

    public Department create(Department d) {
        d.setId(null);
        d.setHotelId(hotelId);
        d.setCreatedAt(LocalDateTime.now());
        departmentMapper.insert(d);
        return d;
    }

    public Department update(Department d) {
        if (d.getId() == null) throw new BusinessException(400, "id 必填");
        departmentMapper.updateById(d);
        return d;
    }

    public void delete(Long id) {
        departmentMapper.deleteById(id);
    }
}
