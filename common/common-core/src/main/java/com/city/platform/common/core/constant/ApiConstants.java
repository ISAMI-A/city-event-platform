package com.city.platform.common.core.constant;

/**
 * 跨服务统一的请求头与路径常量。
 */
public final class ApiConstants {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    public static final String API_PREFIX = "/api/v1";
    public static final String INTERNAL_PREFIX = "/internal/v1";
    public static final String SYSTEM_PING_PATH = "/api/v1/system/ping";

    private ApiConstants() {
    }
}
