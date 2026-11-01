package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkerCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8-32 之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    @NotBlank(message = "部门不能为空")
    private String deptCode;

    @Size(max = 32, message = "区域编码长度不能超过 32")
    private String areaCode;

    /** LEADER 组长 / MEMBER 组员，缺省 MEMBER */
    private String workerType;

    /** 组员所属组长用户ID */
    private Long leaderId;

    @Size(max = 64, message = "片区名称长度不能超过 64")
    private String workZone;

    private BigDecimal zoneLng;

    private BigDecimal zoneLat;
}
