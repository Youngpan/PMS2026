package com.pms.security;

import lombok.Data;

/**
 * 登录上下文（线程局部），由 JwtAuthInterceptor 注入。
 */
public class StaffContext {
    private static final ThreadLocal<StaffPrincipal> HOLDER = new ThreadLocal<>();

    public static void set(StaffPrincipal p) { HOLDER.set(p); }
    public static StaffPrincipal get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    @Data
    public static class StaffPrincipal {
        private final long staffId;
        private final String username;
        private final String realName;
        public StaffPrincipal(long staffId, String username, String realName) {
            this.staffId = staffId; this.username = username; this.realName = realName;
        }
    }
}
