package com.pms.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pms.module.system.OperationLog;
import com.pms.module.system.OperationLogMapper;
import com.pms.security.StaffContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面：拦截 @OpLog 标注的写操作，落库 operation_log。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        Object ret = pjp.proceed();
        try {
            OperationLog entity = new OperationLog();
            entity.setStaffId(StaffContext.get() != null ? StaffContext.get().getStaffId() : 0L);
            entity.setModule(opLog.module());
            entity.setAction(opLog.action());
            entity.setDetail(serialize(pjp.getArgs()));
            entity.setIp(currentIp());
            entity.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("操作日志记录失败: {}", e.getMessage());
        }
        return ret;
    }

    private String serialize(Object[] args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (Exception e) {
            return "[unserializable]";
        }
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            return (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
