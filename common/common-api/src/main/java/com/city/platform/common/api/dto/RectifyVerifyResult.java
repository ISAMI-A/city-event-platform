package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 整改核实结果：模型输出经后端校验后的结构化结论，仅作为管理员验收参考。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RectifyVerifyResult {

    /** RECTIFIED / PARTIALLY_RECTIFIED / NOT_RECTIFIED / UNCERTAIN */
    private String conclusion;
    private BigDecimal confidence;
    private String reason;
    private List<String> issues;
    private boolean degraded;
    private String degradeReason;
    private String model;
    private LocalDateTime verifiedAt;
}
