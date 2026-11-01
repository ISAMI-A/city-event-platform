package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 工单状态变更消息（RabbitMQ）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderStatusMessage implements Serializable {

    private String messageId;
    private String traceId;
    private Long workOrderId;
    private String workOrderNo;
    private Long eventId;
    private String fromStatus;
    private String toStatus;
    private String eventTargetStatus;
    private Long operatorId;
    private String occurredAt;
    private int version;
}
