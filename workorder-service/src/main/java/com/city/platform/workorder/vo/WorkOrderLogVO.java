package com.city.platform.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderLogVO {

    private Long id;
    private String action;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String operatorRole;
    private String content;
    private String attachmentIds;
    private LocalDateTime createTime;
}
