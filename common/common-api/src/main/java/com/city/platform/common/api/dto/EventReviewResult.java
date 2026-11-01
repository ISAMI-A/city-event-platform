package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 事件紧急度复评结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventReviewResult implements Serializable {

    /** LOW / MEDIUM / HIGH / URGENT */
    private String priority;
    private String reason;
    /** 影响范围描述 */
    private String impact;
    /** 建议响应时限（分钟） */
    private Integer responseMinutes;
    private BigDecimal confidence;
    private String model;
    private boolean degraded;
    private String degradeReason;
}
