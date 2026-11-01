package com.city.platform.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkOrderAcceptanceVO {

    private String result;
    private String remark;
    private Long acceptorId;
    private LocalDateTime createTime;
}
