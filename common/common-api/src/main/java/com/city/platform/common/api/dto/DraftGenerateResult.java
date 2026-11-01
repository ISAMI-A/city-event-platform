package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 生成的处理说明：仅作草稿供处理人修改，前端可一键回填。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftGenerateResult implements Serializable {

    private String content;
    private boolean degraded;
    private String degradeReason;
    private String model;
    private LocalDateTime generatedAt;
}
