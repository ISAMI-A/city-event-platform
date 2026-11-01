package com.city.platform.gateway.constant;

/**
 * 网关侧常量（网关为 WebFlux，不依赖 common-core，避免 Servlet 依赖进入响应式栈）。
 */
public final class GatewayConstants {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    public static final String[] WHITE_LIST = {
            "/api/v1/auth/login",
            "/api/v1/auth/wx-login",
            "/api/v1/auth/wx-bind",
            "/api/v1/auth/wx-register",
            "/api/v1/auth/register",
            "/api/v1/system/ping",
            "/actuator",
            "/favicon.ico"
    };

    public static final int CODE_UNAUTHORIZED = 40101;
    public static final int CODE_FORBIDDEN = 40301;

    private GatewayConstants() {
    }
}
