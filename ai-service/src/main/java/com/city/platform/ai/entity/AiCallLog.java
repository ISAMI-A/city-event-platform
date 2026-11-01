package com.city.platform.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ai_call_log")
public class AiCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String bizType;
    private Long userId;
    private String modelName;
    private String promptVersion;
    private String requestSummary;
    private String responseSummary;
    private Integer parsedOk;
    private String validationResult;
    private String errorCode;
    private Integer latencyMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private LocalDateTime createTime;
}
