package com.city.platform.event.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MapPointVO {

    private Long id;
    private String eventNo;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String categoryCode;
    private String status;
    private String priority;
    private String address;
}
