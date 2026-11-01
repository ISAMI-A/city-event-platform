package com.city.platform.workorder.dto;

import com.city.platform.common.core.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderQuery extends PageQuery {

    private String status;

    private Long assigneeId;

    private String eventNo;

    private String categoryCode;

    /** 排序字段：priority（按紧急程度），其他值按创建时间倒序 */
    private String sortBy;

    /** asc / desc */
    private String order;
}
