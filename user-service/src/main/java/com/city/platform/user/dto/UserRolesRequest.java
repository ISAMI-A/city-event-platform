package com.city.platform.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UserRolesRequest {

    @NotEmpty(message = "角色不能为空")
    private List<String> roles;
}
