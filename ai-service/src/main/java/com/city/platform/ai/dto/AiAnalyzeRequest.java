package com.city.platform.ai.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AiAnalyzeRequest {

    /**
     * 用户草稿，可为空：为空时由 AI 根据照片生成描述。
     */
    @Size(max = 1000, message = "事件描述长度不能超过 1000")
    private String description;

    @Size(max = 255, message = "地址长度不能超过 255")
    private String address;

    @Size(max = 32, message = "类别提示长度不能超过 32")
    private String categoryHint;

    /**
     * 已上传附件的访问地址，最多取前 3 张用于视觉分析。
     */
    @Size(max = 9, message = "照片数量不能超过 9 张")
    private List<String> imageUrls;
}
