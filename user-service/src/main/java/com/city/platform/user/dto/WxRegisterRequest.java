package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 居民首次微信登录：创建居民账号。
 */
@Data
public class WxRegisterRequest {

    @NotBlank(message = "绑定凭证不能为空")
    @Size(max = 64, message = "绑定凭证不合法")
    private String bindTicket;

    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;
}
