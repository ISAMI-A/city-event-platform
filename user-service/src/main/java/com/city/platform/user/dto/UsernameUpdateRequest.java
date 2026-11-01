package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户修改登录账号：全局唯一，已被占用时提示更换。
 */
@Data
public class UsernameUpdateRequest {

    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,31}$",
            message = "账号需以字母开头，4-32 位字母、数字或下划线")
    private String username;

    @Size(max = 64, message = "密码长度不合法")
    private String password;
}
