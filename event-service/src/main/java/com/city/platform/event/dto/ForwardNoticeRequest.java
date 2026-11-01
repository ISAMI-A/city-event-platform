package com.city.platform.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 组长把提醒转达给具体组员。
 */
@Data
public class ForwardNoticeRequest {

    @NotNull(message = "工单不能为空")
    private Long workOrderId;

    @NotNull(message = "组员不能为空")
    private Long memberId;

    /** 组长补充的要求 */
    private String note;
}
