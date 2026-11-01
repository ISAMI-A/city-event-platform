package com.city.platform.workorder.service;

import com.city.platform.common.api.feign.EventClient;
import com.city.platform.common.api.feign.UserClient;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.LoginUser;
import com.city.platform.workorder.constant.WorkOrderStatus;
import com.city.platform.workorder.entity.WorkOrder;
import com.city.platform.workorder.mapper.WorkOrderAcceptanceMapper;
import com.city.platform.workorder.mapper.WorkOrderLogMapper;
import com.city.platform.workorder.mapper.WorkOrderMapper;
import com.city.platform.workorder.mapper.WorkOrderReassignMapper;
import com.city.platform.workorder.mq.WorkOrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 并发接单与越权规则：条件更新受影响行数为 0 时必须返回状态冲突，而不是成功。
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderMapper workOrderMapper;
    @Mock
    private WorkOrderLogMapper workOrderLogMapper;
    @Mock
    private WorkOrderAcceptanceMapper acceptanceMapper;
    @Mock
    private WorkOrderReassignMapper reassignMapper;
    @Mock
    private WorkOrderNoGenerator noGenerator;
    @Mock
    private UserClient userClient;
    @Mock
    private EventClient eventClient;
    @Mock
    private WorkOrderEventPublisher publisher;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private WorkOrderService workOrderService;

    @Test
    void acceptShouldFailWhenConditionalUpdateAffectsNoRow() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setAssigneeId(2L);
        workOrder.setStatus(WorkOrderStatus.PENDING_ACCEPT);
        workOrder.setWorkOrderNo("WO202610210001");
        when(workOrderMapper.selectById(1L)).thenReturn(workOrder);
        when(workOrderMapper.accept(anyLong(), anyLong())).thenReturn(0);

        LoginUser worker = new LoginUser(2L, "李道路", RoleConstants.WORKER);
        BizException exception = assertThrows(BizException.class, () -> workOrderService.accept(1L, worker));

        assertEquals(ErrorCode.WORK_ORDER_ALREADY_ACCEPTED.getCode(), exception.getCode());
    }

    @Test
    void acceptShouldRejectOtherWorkersOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setAssigneeId(99L);
        workOrder.setStatus(WorkOrderStatus.PENDING_ACCEPT);
        when(workOrderMapper.selectById(1L)).thenReturn(workOrder);

        LoginUser worker = new LoginUser(2L, "李道路", RoleConstants.WORKER);
        BizException exception = assertThrows(BizException.class, () -> workOrderService.accept(1L, worker));

        assertEquals(ErrorCode.WORK_ORDER_NOT_OWNED.getCode(), exception.getCode());
    }
}
