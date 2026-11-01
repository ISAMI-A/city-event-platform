package com.city.platform.event.constant;

import java.util.Map;
import java.util.Set;

/**
 * 事件状态与合法迁移，与 docs/requirements.md 第 4 章一致。
 */
public final class EventStatus {

    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String DISPATCHED = "DISPATCHED";
    public static final String PROCESSING = "PROCESSING";
    public static final String PENDING_ACCEPTANCE = "PENDING_ACCEPTANCE";
    public static final String CLOSED = "CLOSED";

    public static final Set<String> CATEGORIES = Set.of(
            "ROAD_FACILITY", "PUBLIC_LIGHTING", "ENVIRONMENT_SANITATION",
            "PUBLIC_FACILITY", "OTHER");

    public static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    public static final Map<String, String> CATEGORY_NAMES = Map.of(
            "ROAD_FACILITY", "道路设施",
            "PUBLIC_LIGHTING", "公共照明",
            "ENVIRONMENT_SANITATION", "环境卫生",
            "PUBLIC_FACILITY", "公共设施",
            "OTHER", "其他");

    private EventStatus() {
    }
}
