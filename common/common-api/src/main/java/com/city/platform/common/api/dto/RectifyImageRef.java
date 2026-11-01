package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 整改核实用的图片引用（只传元数据，图片二进制由 ai-service 从共享数据目录读取）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RectifyImageRef {

    private Long id;
    private String accessUrl;
    private String contentType;
    private Long fileSize;
}
