package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 事件快照，供工单详情展示，避免工单服务直接访问事件库。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventSnapshot implements Serializable {

    private Long eventId;
    private String eventNo;
    private String title;
    private String description;
    private String categoryCode;
    private String priority;
    private String status;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Long reporterId;
    private Integer photoCount;
}
