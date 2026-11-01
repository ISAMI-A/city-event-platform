package com.city.platform.map.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_map_call_log")
public class MapCallLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceId;
    private String callType;
    private String requestParam;
    private Integer success;
    private String errorCode;
    private Integer latencyMs;
    private LocalDateTime createTime;
}
