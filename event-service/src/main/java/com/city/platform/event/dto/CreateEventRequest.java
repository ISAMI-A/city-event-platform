package com.city.platform.event.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateEventRequest {

    @Size(max = 100, message = "标题不能超过 100 字")
    private String title;

    @NotBlank(message = "事件描述不能为空")
    @Size(min = 10, max = 1000, message = "事件描述长度需在 10-1000 之间")
    private String description;

    @NotBlank(message = "事件类别不能为空")
    @Size(max = 32)
    private String categoryCode;

    @NotBlank(message = "优先级不能为空")
    @Size(max = 16)
    private String priority;

    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度不合法")
    @DecimalMax(value = "180.0", message = "经度不合法")
    private BigDecimal longitude;

    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度不合法")
    @DecimalMax(value = "90.0", message = "纬度不合法")
    private BigDecimal latitude;

    @NotBlank(message = "地址不能为空")
    @Size(max = 255, message = "地址不能超过 255 字")
    private String address;

    @Size(max = 32)
    private String areaCode;

    @Size(max = 9, message = "最多上传 9 张照片")
    private List<Long> attachmentIds;

    private Long aiCallId;

    /** 管理员代报时可指定上报居民，缺省记为管理员本人 */
    private Long reporterId;
}
