package com.city.platform.common.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 事件维度的工单与评价摘要：供事件列表展示星级、定位责任组长。
 */
@Data
public class EventRating implements Serializable {

    private Long eventId;
    private Long workOrderId;
    private String workOrderNo;
    private String workOrderStatus;
    private Long leaderId;
    private String leaderName;
    private Long assigneeId;
    private String assigneeName;
    /** 1-5 星，未评价为 null */
    private Integer rating;
    private String tags;
    private String comment;
}
