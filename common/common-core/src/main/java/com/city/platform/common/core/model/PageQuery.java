package com.city.platform.common.core.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用分页参数：page 从 1 开始，size 上限 100。
 */
@Data
public class PageQuery implements Serializable {

    @Min(value = 1, message = "page 最小为 1")
    private long page = 1;

    @Min(value = 1, message = "size 最小为 1")
    @Max(value = 100, message = "size 最大为 100")
    private long size = 10;
}
