package com.city.platform.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderListItemVO {

    private Long id;
    private String workOrderNo;
    private Long eventId;
    private String eventNo;
    private String categoryCode;
    private String priority;
    private String status;
    private Long assigneeId;
    private String assigneeName;
    private Long leaderId;
    private String leaderName;
    private LocalDateTime assignTime;
    private LocalDateTime submitTime;
    private LocalDateTime finishTime;
}
