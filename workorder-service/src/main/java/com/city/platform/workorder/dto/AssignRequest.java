package com.city.platform.workorder.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssignRequest {

    @NotNull(message = "请选择组员")
    private Long assigneeId;

    @Size(max = 255, message = "分配说明长度不能超过 255")
    private String remark;
}
