package com.city.platform.event.dto;

import com.city.platform.common.core.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class EventQuery extends PageQuery {

    private String status;

    private String categoryCode;

    private String areaCode;

    private String keyword;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /** 排序字段：priority（按紧急程度），其他值按创建时间倒序 */
    private String sortBy;

    /** asc / desc */
    private String order;
}
