package com.city.platform.workorder.service;

import com.city.platform.workorder.constant.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkOrderStateMachineTest {

    @Test
    void shouldAllowLegalTransitions() {
        assertTrue(WorkOrderStatus.canTransit(WorkOrderStatus.PENDING_ACCEPT, WorkOrderStatus.PROCESSING));
        assertTrue(WorkOrderStatus.canTransit(WorkOrderStatus.PROCESSING, WorkOrderStatus.PENDING_ACCEPTANCE));
        assertTrue(WorkOrderStatus.canTransit(WorkOrderStatus.PENDING_ACCEPTANCE, WorkOrderStatus.COMPLETED));
        assertTrue(WorkOrderStatus.canTransit(WorkOrderStatus.PENDING_ACCEPTANCE, WorkOrderStatus.PROCESSING));
    }

    @Test
    void shouldRejectIllegalTransitions() {
        assertFalse(WorkOrderStatus.canTransit(WorkOrderStatus.PENDING_ACCEPT, WorkOrderStatus.COMPLETED));
        assertFalse(WorkOrderStatus.canTransit(WorkOrderStatus.COMPLETED, WorkOrderStatus.PROCESSING));
        assertFalse(WorkOrderStatus.canTransit(WorkOrderStatus.PROCESSING, WorkOrderStatus.PENDING_ACCEPT));
    }
}
