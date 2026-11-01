package com.city.platform.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuditRejectRequest {

    @NotBlank(message = "驳回原因不能为空")
    @Size(min = 5, max = 200, message = "驳回原因长度需在 5-200 之间")
    private String reason;
}
