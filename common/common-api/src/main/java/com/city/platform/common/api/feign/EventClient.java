package com.city.platform.common.api.feign;

import com.city.platform.common.api.dto.BindAttachmentCommand;
import com.city.platform.common.api.dto.EventSnapshot;
import com.city.platform.common.api.dto.RectifyImageRef;
import com.city.platform.common.core.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * event-service 内部契约：事件快照与附件绑定。
 */
@FeignClient(name = "event-service", contextId = "eventClient", path = "/internal/v1/events",
        url = "${city.feign.event-service-url:}")
public interface EventClient {

    @GetMapping("/{eventId}/snapshot")
    R<EventSnapshot> snapshot(@PathVariable("eventId") Long eventId);

    @PostMapping("/attachments/bind")
    R<Void> bindAttachments(@RequestBody BindAttachmentCommand command);

    /**
     * 按 ID 批量读取附件元数据（供 AI 核实构造图片引用）。
     */
    @GetMapping("/attachments")
    R<List<RectifyImageRef>> attachments(@RequestParam("ids") String ids);

    /**
     * 事件上报时绑定的原始照片，供工单详情展示与 AI 整改前后对照。
     */
    @GetMapping("/{eventId}/attachments")
    R<List<RectifyImageRef>> eventAttachments(@PathVariable("eventId") Long eventId);

    /**
     * 工单被删除后，事件退回待审核。
     */
    @PostMapping("/{eventId}/rollback-dispatch")
    R<Void> rollbackDispatch(@PathVariable("eventId") Long eventId);
}
