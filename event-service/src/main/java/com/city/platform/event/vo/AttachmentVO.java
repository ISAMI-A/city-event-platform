package com.city.platform.event.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentVO {

    private Long id;
    private String originalName;
    private String accessUrl;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createTime;
}
