package com.city.platform.event.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员提醒责任组长跟进居民评价。
 */
@Data
public class RemindRequest {

    @NotNull(message = "事件不能为空")
    private Long eventId;

    /** 可选补充说明，会拼入通知内容 */
    private String note;
}
