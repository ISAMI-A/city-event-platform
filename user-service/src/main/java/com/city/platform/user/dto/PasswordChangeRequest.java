package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户设置 / 修改密码：未设置过密码时可不传原密码。
 */
@Data
public class PasswordChangeRequest {

    @Size(max = 64, message = "原密码长度不合法")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需在 8-32 之间")
    private String newPassword;
}
