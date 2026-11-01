package com.city.platform.workorder.constant;

import java.util.Map;
import java.util.Set;

public final class WorkOrderStatus {

    public static final String PENDING_ACCEPT = "PENDING_ACCEPT";
    public static final String PROCESSING = "PROCESSING";
    /** 组员提交后等待组长复核 */
    public static final String PENDING_LEADER_REVIEW = "PENDING_LEADER_REVIEW";
    /** 组长复核通过/组长本人处理后等待管理员验收 */
    public static final String PENDING_ACCEPTANCE = "PENDING_ACCEPTANCE";
    public static final String COMPLETED = "COMPLETED";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            PENDING_ACCEPT, Set.of(PROCESSING),
            PROCESSING, Set.of(PENDING_LEADER_REVIEW, PENDING_ACCEPTANCE, PENDING_ACCEPT),
            PENDING_LEADER_REVIEW, Set.of(PENDING_ACCEPTANCE, PROCESSING),
            PENDING_ACCEPTANCE, Set.of(PROCESSING, COMPLETED)
    );

    private WorkOrderStatus() {
    }

    public static boolean canTransit(String from, String to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
