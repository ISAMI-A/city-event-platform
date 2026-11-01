package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 整改核实请求：居民上报的原始事件信息 + 处理人提交的整改说明与现场照片。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RectifyVerifyRequest {

    private Long workOrderId;
    private String workOrderNo;
    private String eventNo;
    private String categoryCode;
    private String eventTitle;
    private String eventAddress;
    private String eventDescription;
    private String rectifyContent;
    /** 居民上报时的原始照片，作为“整改前”参照 */
    private List<RectifyImageRef> originalImages;
    /** 处理人提交的整改后照片 */
    private List<RectifyImageRef> images;
}
