package com.city.platform.common.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 附件绑定命令：工单提交结果时通过内部接口把附件归属到工单。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindAttachmentCommand implements Serializable {

    private List<Long> attachmentIds;
    private Long bizId;
    private Long ownerId;
    private String bizType;
}
