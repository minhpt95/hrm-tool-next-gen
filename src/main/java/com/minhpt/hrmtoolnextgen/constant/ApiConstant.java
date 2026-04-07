package com.minhpt.hrmtoolnextgen.constant;

public final class ApiConstant {
    public static final String API_PREFIX = "/api";
    public static final String API_V1_PREFIX = "/api/v1";

    public static final String ADMIN_BASE = API_PREFIX + "/admin";
    public static final String ADMIN_V1_BASE = API_V1_PREFIX + "/admin";
    public static final String ADMIN_DASHBOARD_BASE = ADMIN_BASE + "/dashboard";
    public static final String ADMIN_DASHBOARD_V1_BASE = ADMIN_V1_BASE + "/dashboard";

    public static final String AUTH_BASE = API_PREFIX + "/auth";
    public static final String AUTH_V1_BASE = API_V1_PREFIX + "/auth";

    public static final String USER_BASE = API_PREFIX + "/user";
    public static final String USER_V1_BASE = API_V1_PREFIX + "/user";

    public static final String MANAGER_BASE = API_PREFIX + "/manager";
    public static final String MANAGER_V1_BASE = API_V1_PREFIX + "/manager";

    public static final String HOLIDAYS_BASE = API_PREFIX + "/holidays";
    public static final String HOLIDAYS_V1_BASE = API_V1_PREFIX + "/holidays";

    public static final String SSE_BASE = "/sse";
    public static final String SSE_V1_BASE = API_V1_PREFIX + "/sse";

    private ApiConstant() {
    }
}
