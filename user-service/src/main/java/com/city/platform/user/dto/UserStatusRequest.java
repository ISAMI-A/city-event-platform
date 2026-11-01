package com.city.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserStatusRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}
