package com.city.platform.workorder.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 生成处理说明草稿：只取第一张已上传的整改后照片，减少模型耗时。
 */
@Data
public class DraftRequest {

    @NotEmpty(message = "请先上传至少 1 张整改后照片")
    @Size(max = 9, message = "最多 9 张照片")
    private List<Long> attachmentIds;
}
