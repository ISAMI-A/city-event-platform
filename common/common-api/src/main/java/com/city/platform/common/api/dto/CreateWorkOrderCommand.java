package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 内部创建工单命令。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkOrderCommand implements Serializable {

    private Long eventId;
    private String eventNo;
    private String categoryCode;
    private String priority;
    private Long assigneeId;
    private String assigneeName;
    private String deptCode;
    private Long creatorId;
}
