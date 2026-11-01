package com.city.platform.event.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventListItemVO {

    private Long id;
    private String eventNo;
    private String title;
    private String categoryCode;
    private String priority;
    private String aiReviewPriority;
    private String status;
    private String district;
    private String address;
    private Integer photoCount;
    private String rejectReason;
    private LocalDateTime createTime;

    /** 是否管理员待办（待审核） */
    private Boolean pending;
    private Long workOrderId;
    private String workOrderStatus;
    private Long leaderId;
    private String leaderName;
    private String assigneeName;
    /** 居民评分 1-5，未评价为空 */
    private Integer rating;
    private String ratingTags;
    private String ratingComment;
}
