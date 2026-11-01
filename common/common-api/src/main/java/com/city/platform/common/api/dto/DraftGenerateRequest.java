package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 生成处理说明请求：只带整改前的文字描述与 1 张整改后照片，控制模型耗时。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftGenerateRequest implements Serializable {

    private String workOrderNo;
    private String eventNo;
    private String categoryCode;
    private String eventTitle;
    private String eventAddress;
    private String eventDescription;
    private RectifyImageRef image;
}
