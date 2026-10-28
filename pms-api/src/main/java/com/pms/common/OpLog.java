package com.pms.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注写操作，由 OperationLogAspect 记录到 operation_log。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {
    /** 模块，如 M1/M2 */
    String module();
    /** 动作，如 入住/退房/收款 */
    String action();
}
