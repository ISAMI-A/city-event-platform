package com.city.platform.event.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ResubmitEventRequest {

    @NotBlank(message = "事件描述不能为空")
    @Size(min = 10, max = 500, message = "事件描述长度需在 10-500 之间")
    private String description;

    @NotBlank(message = "事件类别不能为空")
    @Size(max = 32)
    private String categoryCode;

    @NotBlank(message = "优先级不能为空")
    @Size(max = 16)
    private String priority;

    @DecimalMin(value = "-180.0", message = "经度不合法")
    @DecimalMax(value = "180.0", message = "经度不合法")
    private BigDecimal longitude;

    @DecimalMin(value = "-90.0", message = "纬度不合法")
    @DecimalMax(value = "90.0", message = "纬度不合法")
    private BigDecimal latitude;

    @Size(max = 255, message = "地址不能超过 255 字")
    private String address;

    @Size(max = 32)
    private String areaCode;

    @Size(max = 9, message = "最多上传 9 张照片")
    private List<Long> attachmentIds;
}
