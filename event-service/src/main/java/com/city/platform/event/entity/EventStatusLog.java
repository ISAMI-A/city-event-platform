package com.city.platform.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_event_status_log")
public class EventStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long eventId;
    private String eventNo;
    private String fromStatus;
    private String toStatus;
    private String action;
    private Long operatorId;
    private String operatorRole;
    private String remark;
    private LocalDateTime createTime;
}
