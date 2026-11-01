package com.city.platform.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalyzeResponse {

    private Long callId;
    private String categoryCode;
    /**
     * 润色扩写后的事件描述，可直接用于上报；降级时为空。
     */
    private String description;
    private String summary;
    private String priority;
    private String suggestion;
    private String reasoning;
    private BigDecimal confidence;
    private boolean degraded;
    private String degradeReason;
    private String model;
}
