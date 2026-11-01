package com.city.platform.event.vo;

import com.city.platform.common.api.dto.WorkOrderBrief;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventDetailVO {

    private Long id;
    private String eventNo;
    private Long reporterId;
    private String title;
    private String description;
    private String summary;
    private String categoryCode;
    private String aiCategoryCode;
    private String priority;
    private String aiPriority;
    private BigDecimal aiConfidence;
    private String aiSuggestion;
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
    private LocalDateTime createTime;

    private List<AttachmentVO> attachments;
    private List<TimelineItemVO> timeline;
    private AiSnapshotVO aiSnapshot;
    private WorkOrderBrief workOrder;
}
