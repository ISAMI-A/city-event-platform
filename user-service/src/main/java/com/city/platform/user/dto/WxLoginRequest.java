package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WxLoginRequest {

    @NotBlank(message = "微信登录凭证不能为空")
    @Size(max = 128, message = "微信登录凭证长度不能超过 128")
    private String code;
}
