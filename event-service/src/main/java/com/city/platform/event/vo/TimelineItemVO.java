package com.city.platform.event.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimelineItemVO {

    private String fromStatus;
    private String toStatus;
    private String action;
    private String operatorRole;
    private String remark;
    private LocalDateTime createTime;
}
