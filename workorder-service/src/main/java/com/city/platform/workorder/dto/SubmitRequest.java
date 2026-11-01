package com.city.platform.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SubmitRequest {

    @NotBlank(message = "处理说明不能为空")
    @Size(min = 10, max = 1000, message = "处理说明长度需在 10-1000 之间")
    private String content;

    @NotEmpty(message = "至少上传 1 张现场照片")
    @Size(max = 9, message = "最多 9 张照片")
    private List<Long> attachmentIds;
}
