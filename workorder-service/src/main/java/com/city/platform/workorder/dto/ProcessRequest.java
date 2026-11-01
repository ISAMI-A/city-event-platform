package com.city.platform.workorder.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProcessRequest {

    @Size(max = 1000, message = "处理说明不能超过 1000 字")
    private String content;

    @Size(max = 9, message = "最多 9 张照片")
    private List<Long> attachmentIds;
}
