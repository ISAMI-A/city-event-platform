package com.city.platform.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_work_order_log")
public class WorkOrderLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workOrderId;
    private String workOrderNo;
    private String action;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String operatorRole;
    private String content;
    private String attachmentIds;
    private LocalDateTime createTime;
}
