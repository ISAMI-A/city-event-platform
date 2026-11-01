package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 逆地址解析结果（GCJ-02）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoResult implements Serializable {

    private String province;
    private String city;
    private String district;
    private String street;
    private String areaCode;
    private String address;
    private String formattedAddress;

    /**
     * 外部地图不可用或解析失败时为 true，调用方保留用户填写的地址。
     */
    private boolean pending;
}
