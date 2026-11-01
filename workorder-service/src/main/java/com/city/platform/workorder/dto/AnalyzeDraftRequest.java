package com.city.platform.workorder.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 工人提交前让 AI 预检草稿：说明与照片都来自尚未提交的编辑态。
 */
@Data
public class AnalyzeDraftRequest {

    @Size(max = 1000, message = "处理说明不超过 1000 字")
    private String content;

    @Size(max = 9, message = "最多 9 张照片")
    private List<Long> attachmentIds;
}
