package com.city.platform.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerUpdateRequest {

    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    private String deptCode;

    @Size(max = 32, message = "区域编码长度不能超过 32")
    private String areaCode;

    private String status;

    /** LEADER / MEMBER */
    private String workerType;

    /** 组员所属组长用户ID；传 0 表示清空 */
    private Long leaderId;

    @Size(max = 64, message = "片区名称长度不能超过 64")
    private String workZone;

    private BigDecimal zoneLng;

    private BigDecimal zoneLat;
}
