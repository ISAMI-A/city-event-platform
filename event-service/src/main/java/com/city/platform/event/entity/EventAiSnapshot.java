package com.city.platform.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_event_ai_snapshot")
public class EventAiSnapshot {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long eventId;
    private Long aiCallLogId;
    private String categoryCode;
    private String priority;
    private String summary;
    private String suggestion;
    private BigDecimal confidence;
    private String modelName;
    private Integer accepted;
    private LocalDateTime createTime;
}
