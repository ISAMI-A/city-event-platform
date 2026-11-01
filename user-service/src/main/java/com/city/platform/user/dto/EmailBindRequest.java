package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 绑定 / 更换邮箱：邮箱 + 收到的验证码；更换已绑定邮箱时需校验当前登录密码。
 */
@Data
public class EmailBindRequest {

    @NotBlank(message = "邮箱不能为空")
    @Size(max = 128, message = "邮箱长度不能超过 128")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码为 6 位数字")
    private String code;

    @Size(max = 64, message = "密码长度不合法")
    private String password;
}
