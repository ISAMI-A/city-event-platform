package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 工单分配通知消息：业务唯一标识为 workOrderId + messageId。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderAssignedMessage implements Serializable {

    private String messageId;
    private String traceId;
    private Long workOrderId;
    private String workOrderNo;
    private Long eventId;
    private String eventNo;
    private Long assigneeId;
    private String assigneeName;
    private Long assignedBy;
    private String occurredAt;
}
