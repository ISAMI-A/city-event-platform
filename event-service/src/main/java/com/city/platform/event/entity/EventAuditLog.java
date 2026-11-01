package com.city.platform.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_event_audit_log")
public class EventAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long eventId;
    private Long auditorId;
    private String decision;
    private String beforeCategory;
    private String afterCategory;
    private String beforePriority;
    private String afterPriority;
    private String reason;
    private LocalDateTime createTime;
}
