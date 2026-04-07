package com.minhpt.hrmtoolnextgen.constant;

public final class ApiConstant {
    public static final String API_PREFIX = "/api";
    public static final String API_V1_PREFIX = "/api/v1";
    public static final String LEGACY_API_SUNSET = "Wed, 31 Dec 2026 23:59:59 GMT";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    public static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";
    public static final String API_DOCS_PATTERN = "/v3/api-docs/**";

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

        public static final String[] SWAGGER_ENDPOINTS = {
            SWAGGER_UI_HTML,
            SWAGGER_UI_PATTERN,
            API_DOCS_PATTERN
        };

        public static final String[] AUTH_ENDPOINTS = {
            AUTH_BASE + "/**",
            AUTH_V1_BASE + "/**"
        };

        public static final String[] ADMIN_ENDPOINTS = {
            ADMIN_BASE,
            ADMIN_BASE + "/**",
            ADMIN_V1_BASE,
            ADMIN_V1_BASE + "/**"
        };

        public static final String[] MANAGER_ENDPOINTS = {
            MANAGER_BASE,
            MANAGER_BASE + "/**",
            MANAGER_V1_BASE,
            MANAGER_V1_BASE + "/**"
        };

        public static final String[] USER_ENDPOINTS = {
            USER_BASE,
            USER_BASE + "/**",
            USER_V1_BASE,
            USER_V1_BASE + "/**",
            SSE_BASE,
            SSE_BASE + "/**",
            SSE_V1_BASE,
            SSE_V1_BASE + "/**"
        };

        public static final String[] HOLIDAY_ENDPOINTS = {
            HOLIDAYS_BASE,
            HOLIDAYS_BASE + "/**",
            HOLIDAYS_V1_BASE,
            HOLIDAYS_V1_BASE + "/**"
        };

    public static boolean isLegacyPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return (path.startsWith(API_PREFIX + "/") && !path.startsWith(API_V1_PREFIX + "/"))
                || SSE_BASE.equals(path)
                || path.startsWith(SSE_BASE + "/");
    }

    public static String toVersionedPath(String path) {
        if (path == null || path.isBlank()) {
            return API_V1_PREFIX;
        }
        if (path.startsWith(API_V1_PREFIX)) {
            return path;
        }
        if (SSE_BASE.equals(path) || path.startsWith(SSE_BASE + "/")) {
            return API_V1_PREFIX + path;
        }
        if (path.startsWith(API_PREFIX)) {
            return API_V1_PREFIX + path.substring(API_PREFIX.length());
        }
        return path;
    }

    private ApiConstant() {
    }
}
