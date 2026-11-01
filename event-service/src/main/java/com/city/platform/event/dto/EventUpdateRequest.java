package com.city.platform.event.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员修改事件基础信息：仅待审核事件可编辑。
 */
@Data
public class EventUpdateRequest {

    @Size(max = 100, message = "标题不能超过 100 字")
    private String title;

    @Size(min = 10, max = 1000, message = "事件描述长度需在 10-1000 之间")
    private String description;

    @Size(max = 32)
    private String categoryCode;

    @Size(max = 16)
    private String priority;

    @Size(max = 255, message = "地址不能超过 255 字")
    private String address;

    @Size(max = 32)
    private String areaCode;
}
