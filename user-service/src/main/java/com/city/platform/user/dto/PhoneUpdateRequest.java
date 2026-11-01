package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户绑定 / 更换手机号（仅唯一校验，不做短信验证）。
 * 更换已绑定手机号时需校验当前登录密码。
 */
@Data
public class PhoneUpdateRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的 11 位手机号")
    private String phone;

    @Size(max = 64, message = "密码长度不合法")
    private String password;
}
