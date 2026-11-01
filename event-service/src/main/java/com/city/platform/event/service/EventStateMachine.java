package com.city.platform.event.service;

import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.event.constant.EventStatus;

import java.util.Map;
import java.util.Set;

/**
 * 事件状态机：只允许白名单内的迁移。
 */
public final class EventStateMachine {

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            EventStatus.PENDING_REVIEW, Set.of(EventStatus.DISPATCHED, EventStatus.REJECTED, EventStatus.CANCELLED),
            EventStatus.REJECTED, Set.of(EventStatus.PENDING_REVIEW),
            EventStatus.DISPATCHED, Set.of(EventStatus.PROCESSING, EventStatus.PENDING_REVIEW),
            EventStatus.PROCESSING, Set.of(EventStatus.PENDING_ACCEPTANCE),
            EventStatus.PENDING_ACCEPTANCE, Set.of(EventStatus.PROCESSING, EventStatus.CLOSED)
    );

    private EventStateMachine() {
    }

    public static boolean canTransit(String from, String to) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public static void assertTransit(String from, String to) {
        if (!canTransit(from, to)) {
            throw new BizException(ErrorCode.EVENT_STATE_NOT_ALLOWED,
                    "事件状态不允许从 " + from + " 变更为 " + to);
        }
    }
}
