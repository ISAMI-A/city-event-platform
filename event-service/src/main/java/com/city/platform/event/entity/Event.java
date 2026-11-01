package com.city.platform.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.city.platform.common.core.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_event")
public class Event extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventNo;
    private Long reporterId;
    private String categoryCode;
    private String aiCategoryCode;
    private String title;
    private String description;
    private String summary;
    private String priority;
    private String aiPriority;
    private BigDecimal aiConfidence;
    private String aiSuggestion;
    /** AI 审核复评的紧急程度与依据（管理员审核前生成） */
    private String aiReviewPriority;
    private String aiReviewReason;
    private String aiReviewImpact;
    private Integer aiReviewResponseMinutes;
    private BigDecimal aiReviewConfidence;
    private LocalDateTime aiReviewTime;
    private String status;
    private String province;
    private String city;
    private String district;
    private String street;
    private String areaCode;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer photoCount;
    private Long auditorId;
    private LocalDateTime auditTime;
    private String rejectReason;
    private LocalDateTime dispatchTime;
    private LocalDateTime closeTime;
}
