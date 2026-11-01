package com.city.platform.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 组长向本组组员发布注意事项。
 */
@Data
public class TeamNoticeRequest {

    @NotBlank(message = "注意事项内容不能为空")
    @Size(max = 400, message = "注意事项不能超过 400 字")
    private String content;
}
