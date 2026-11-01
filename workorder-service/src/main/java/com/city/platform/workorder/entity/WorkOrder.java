package com.city.platform.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.platform.common.core.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_work_order")
public class WorkOrder extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String workOrderNo;
    private Long eventId;
    private String eventNo;
    private String categoryCode;
    private String priority;
    private Long assigneeId;
    private String assigneeName;
    /** 责任组长（创建时即派单对象） */
    private Long leaderId;
    private String leaderName;
    private String deptCode;
    private String status;
    private Long creatorId;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime submitTime;
    private LocalDateTime leaderReviewTime;
    private String leaderReviewRemark;
    private LocalDateTime finishTime;
    private String acceptanceRemark;
    private Integer returnCount;
    private Integer reassignCount;
    /** AI 整改核实结果（JSON） */
    private String aiVerifyResult;
    private LocalDateTime aiVerifyTime;
}
