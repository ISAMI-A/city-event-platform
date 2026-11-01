package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 服务存活探测结果（真实服务返回，非本地伪造）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PingResult implements Serializable {

    private String service;
    private String instance;
    private String timestamp;
    private String traceId;
}
