package com.city.platform.common.core.web;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * traceId 工具：请求入口写入 MDC，Feign 与 MQ 透传。
 */
public final class TraceIdUtils {

    public static final String TRACE_ID_KEY = "traceId";

    private TraceIdUtils() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    public static void clear() {
        MDC.remove(TRACE_ID_KEY);
    }
}
