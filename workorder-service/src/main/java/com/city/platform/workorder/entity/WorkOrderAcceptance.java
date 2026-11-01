package com.city.platform.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_work_order_acceptance")
public class WorkOrderAcceptance {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workOrderId;
    private String workOrderNo;
    private Long acceptorId;
    private String result;
    private String remark;
    private LocalDateTime createTime;
}
