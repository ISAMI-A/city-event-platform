package com.city.platform.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户资料更新：仅允许修改昵称与头像。
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 64, message = "昵称长度不能超过 64")
    private String nickname;

    @Size(max = 512, message = "头像地址长度不能超过 512")
    private String avatarUrl;
}
