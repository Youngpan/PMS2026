package com.pms.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注写操作支持幂等：客户端带 Idempotency-Key 头，
 * 相同 key+请求在 ttl 内命中缓存直接返回上次结果。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /** 缓存有效期（秒），默认 60 */
    int ttl() default 60;
}
