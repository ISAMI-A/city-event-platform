package com.city.platform.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuditApproveRequest {

    @NotNull(message = "处理人员不能为空")
    private Long workerId;

    @NotBlank(message = "审核类别不能为空")
    @Size(max = 32)
    private String categoryCode;

    @NotBlank(message = "审核优先级不能为空")
    @Size(max = 16)
    private String priority;

    @Size(max = 32)
    private String areaCode;

    @Size(max = 255, message = "备注不能超过 255 字")
    private String remark;
}
