package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 已登录账号绑定微信：小程序 wx.login 得到的一次性 code。
 */
@Data
public class WxCodeRequest {

    @NotBlank(message = "微信登录凭证不能为空")
    @Size(max = 128, message = "微信登录凭证不合法")
    private String code;
}
