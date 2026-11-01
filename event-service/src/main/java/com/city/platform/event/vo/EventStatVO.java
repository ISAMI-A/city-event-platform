package com.city.platform.event.vo;

import lombok.Data;

@Data
public class EventStatVO {

    private long total;
    private long pendingReview;
    private long dispatched;
    private long processing;
    private long pendingAcceptance;
    private long closed;
    private long rejected;
    private long cancelled;
}
