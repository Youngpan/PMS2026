package com.pms.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等切面：依据 Idempotency-Key 头缓存 @Idempotent 方法的返回值。
 * 单机内存缓存，符合轻量单机部署；如需多实例可换 Redis。
 */
@Slf4j
@Aspect
@Component
public class IdempotencyAspect {

    private static final ConcurrentHashMap<String, Cached> CACHE = new ConcurrentHashMap<>();
    private static final int MAX_SIZE = 10_000;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        HttpServletRequest req = currentRequest();
        if (req == null) return pjp.proceed();

        String key = req.getHeader("Idempotency-Key");
        if (key == null || key.isEmpty()) return pjp.proceed();

        String cacheKey = req.getMethod() + ":" + req.getRequestURI() + ":" + key;
        long now = System.currentTimeMillis();
        Cached hit = CACHE.get(cacheKey);
        if (hit != null) {
            if (hit.expiresAt > now) {
                log.info("幂等命中: {}", cacheKey);
                return hit.value;
            }
            CACHE.remove(cacheKey);
        }

        Object ret = pjp.proceed();
        if (CACHE.size() < MAX_SIZE) {
            CACHE.put(cacheKey, new Cached(ret, now + idempotent.ttl() * 1000L));
        }
        return ret;
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private record Cached(Object value, long expiresAt) {}
}
