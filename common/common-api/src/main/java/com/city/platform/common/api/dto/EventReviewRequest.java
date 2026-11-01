package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 事件紧急度复评请求：管理员审核前由大模型重新评估紧急程度。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventReviewRequest implements Serializable {

    private String title;
    private String description;
    private String address;
    private String categoryCode;
    /** 居民自选优先级 */
    private String priority;
    private Integer photoCount;
}
