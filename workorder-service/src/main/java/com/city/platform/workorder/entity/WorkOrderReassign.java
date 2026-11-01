package com.city.platform.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_work_order_reassign")
public class WorkOrderReassign {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workOrderId;
    private Long fromAssigneeId;
    private Long toAssigneeId;
    private String reason;
    private Long operatorId;
    private LocalDateTime createTime;
}
