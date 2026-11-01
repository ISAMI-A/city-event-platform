package com.city.platform.workorder.vo;

import com.city.platform.common.api.dto.EventSnapshot;
import com.city.platform.common.api.dto.RectifyImageRef;
import com.city.platform.common.api.dto.RectifyVerifyResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WorkOrderDetailVO {

    private Long id;
    private String workOrderNo;
    private Long eventId;
    private String eventNo;
    private String categoryCode;
    private String priority;
    private String status;
    private Long assigneeId;
    private String assigneeName;
    private Long leaderId;
    private String leaderName;
    private String deptCode;
    private Long creatorId;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime submitTime;
    private LocalDateTime leaderReviewTime;
    private String leaderReviewRemark;
    private LocalDateTime finishTime;
    private String acceptanceRemark;
    private Integer returnCount;
    private Integer reassignCount;

    private EventSnapshot event;
    /** 居民上报时的原始照片，供管理员对照审核 */
    private List<RectifyImageRef> eventPhotos;
    private List<WorkOrderLogVO> logs;
    private WorkOrderAcceptanceVO acceptance;

    /** AI 整改核实结果（仅作验收参考） */
    private RectifyVerifyResult aiVerify;
    private LocalDateTime aiVerifyTime;
    /** 居民评分（完成后可评价一次） */
    private WorkRatingVO rating;
}
