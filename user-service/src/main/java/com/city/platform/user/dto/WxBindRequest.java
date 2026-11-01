package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 工人首次微信登录：用账号密码完成微信绑定。
 */
@Data
public class WxBindRequest {

    @NotBlank(message = "绑定凭证不能为空")
    @Size(max = 64, message = "绑定凭证不合法")
    private String bindTicket;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    private String password;
}
