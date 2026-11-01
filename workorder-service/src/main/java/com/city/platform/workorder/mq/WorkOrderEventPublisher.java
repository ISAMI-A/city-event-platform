package com.city.platform.workorder.mq;

import com.city.platform.common.api.dto.WorkOrderStatusMessage;
import com.city.platform.common.core.web.TraceIdUtils;
import com.city.platform.workorder.config.RabbitConfig;
import com.city.platform.workorder.entity.WorkOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 工单状态消息：在本地事务提交后发送，发送失败不影响已提交的业务数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderEventPublisher {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RabbitTemplate rabbitTemplate;

    public void publishAssigned(WorkOrder workOrder, Long assignedBy) {
        com.city.platform.common.api.dto.WorkOrderAssignedMessage message =
                new com.city.platform.common.api.dto.WorkOrderAssignedMessage(
                        UUID.randomUUID().toString(), TraceIdUtils.getTraceId(),
                        workOrder.getId(), workOrder.getWorkOrderNo(), workOrder.getEventId(),
                        workOrder.getEventNo(), workOrder.getAssigneeId(), workOrder.getAssigneeName(),
                        assignedBy, LocalDateTime.now().format(FORMATTER));

        Runnable send = () -> {
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.WORKORDER_EXCHANGE,
                        RabbitConfig.WORKORDER_ASSIGNED_KEY, message);
                log.info("工单分配消息已发送 workOrderNo={} assigneeId={} messageId={}",
                        workOrder.getWorkOrderNo(), workOrder.getAssigneeId(), message.getMessageId());
            } catch (Exception e) {
                log.error("工单分配消息发送失败 workOrderNo={} messageId={}",
                        workOrder.getWorkOrderNo(), message.getMessageId(), e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    public void publish(WorkOrder workOrder, String fromStatus, String toStatus,
                        String eventTargetStatus, Long operatorId) {
        WorkOrderStatusMessage message = new WorkOrderStatusMessage(
                UUID.randomUUID().toString(),
                TraceIdUtils.getTraceId(),
                workOrder.getId(), workOrder.getWorkOrderNo(), workOrder.getEventId(),
                fromStatus, toStatus, eventTargetStatus, operatorId,
                LocalDateTime.now().format(FORMATTER), 1);

        Runnable send = () -> {
            try {
                rabbitTemplate.convertAndSend(RabbitConfig.WORKORDER_EXCHANGE,
                        RabbitConfig.WORKORDER_STATUS_KEY, message);
                log.info("工单状态消息已发送 workOrderNo={} {}->{} eventTarget={} messageId={}",
                        workOrder.getWorkOrderNo(), fromStatus, toStatus, eventTargetStatus, message.getMessageId());
            } catch (Exception e) {
                log.error("工单状态消息发送失败 workOrderNo={} messageId={}",
                        workOrder.getWorkOrderNo(), message.getMessageId(), e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
