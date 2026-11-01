package com.city.platform.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AcceptanceRequest {

    @NotBlank(message = "验收结果不能为空")
    @Pattern(regexp = "PASS|REJECT", message = "验收结果只能是 PASS 或 REJECT")
    private String result;

    @Size(max = 255, message = "验收意见不能超过 255 字")
    private String remark;
}
