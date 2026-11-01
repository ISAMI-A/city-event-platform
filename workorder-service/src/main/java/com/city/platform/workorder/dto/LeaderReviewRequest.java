package com.city.platform.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeaderReviewRequest {

    /** PASS / REJECT */
    @NotBlank(message = "复核结果不能为空")
    private String result;

    @Size(max = 255, message = "复核意见长度不能超过 255")
    private String remark;
}
