package com.city.platform.event.service;

import com.city.platform.common.core.exception.BizException;
import com.city.platform.event.constant.EventStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventStateMachineTest {

    @Test
    void shouldAllowReviewAndDispatchTransitions() {
        assertTrue(EventStateMachine.canTransit(EventStatus.PENDING_REVIEW, EventStatus.DISPATCHED));
        assertTrue(EventStateMachine.canTransit(EventStatus.PENDING_REVIEW, EventStatus.REJECTED));
        assertTrue(EventStateMachine.canTransit(EventStatus.DISPATCHED, EventStatus.PROCESSING));
        assertTrue(EventStateMachine.canTransit(EventStatus.PENDING_ACCEPTANCE, EventStatus.CLOSED));
        assertTrue(EventStateMachine.canTransit(EventStatus.PENDING_ACCEPTANCE, EventStatus.PROCESSING));
    }

    @Test
    void shouldRejectSkipTransitions() {
        assertFalse(EventStateMachine.canTransit(EventStatus.PENDING_REVIEW, EventStatus.CLOSED));
        assertFalse(EventStateMachine.canTransit(EventStatus.CLOSED, EventStatus.PROCESSING));
        assertThrows(BizException.class, () ->
                EventStateMachine.assertTransit(EventStatus.CLOSED, EventStatus.PENDING_REVIEW));
    }
}
