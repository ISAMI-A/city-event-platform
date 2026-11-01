package com.city.platform.workorder.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkRatingVO {

    private Integer rating;
    private String tags;
    private String comment;
    private LocalDateTime createTime;
}
