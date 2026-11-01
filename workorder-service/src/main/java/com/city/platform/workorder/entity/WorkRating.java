package com.city.platform.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.platform.common.core.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_work_rating")
public class WorkRating extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long workOrderId;
    private String workOrderNo;
    private Long workerId;
    private Long reporterId;
    private Integer rating;
    private String tags;
    private String comment;
}
