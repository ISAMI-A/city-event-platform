package com.city.platform.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_ai_analysis")
public class AiAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long callLogId;
    private String categoryCode;
    private String summary;
    private String priority;
    private String suggestion;
    private String reasoning;
    private BigDecimal confidence;
    private Integer degraded;
    private String degradeReason;
    private String modelName;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
