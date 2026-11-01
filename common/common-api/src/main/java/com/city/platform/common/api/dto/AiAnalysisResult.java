package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * AI 分析结果（已完成后端校验）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResult implements Serializable {

    private Long analysisId;
    private Long callId;
    private String categoryCode;
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
