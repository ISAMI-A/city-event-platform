package com.city.platform.event.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiSnapshotVO {

    private String categoryCode;
    private String priority;
    private String summary;
    private String suggestion;
    private BigDecimal confidence;
    private Integer accepted;
}
