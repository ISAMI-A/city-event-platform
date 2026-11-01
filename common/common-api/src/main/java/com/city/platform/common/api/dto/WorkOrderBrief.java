package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 工单创建结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderBrief implements Serializable {

    private Long workOrderId;
    private String workOrderNo;
    private String status;
    private Long assigneeId;
}
