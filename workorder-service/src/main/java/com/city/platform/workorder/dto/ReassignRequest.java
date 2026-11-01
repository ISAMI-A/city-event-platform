package com.city.platform.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReassignRequest {

    @NotNull(message = "新的处理人不能为空")
    private Long assigneeId;

    @NotBlank(message = "改派原因不能为空")
    @Size(min = 5, max = 200, message = "改派原因长度需在 5-200 之间")
    private String reason;
}
