package com.city.platform.event.controller;

import com.city.platform.common.api.dto.BindAttachmentCommand;
import com.city.platform.common.api.dto.EventSnapshot;
import com.city.platform.common.api.dto.RectifyImageRef;
import com.city.platform.common.core.model.R;
import com.city.platform.event.service.AttachmentService;
import com.city.platform.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/internal/v1/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventService eventService;
    private final AttachmentService attachmentService;

    @GetMapping("/{eventId}/snapshot")
    public R<EventSnapshot> snapshot(@PathVariable("eventId") Long eventId) {
        return R.ok(eventService.snapshot(eventId));
    }

    @PostMapping("/attachments/bind")
    public R<Void> bindAttachments(@RequestBody BindAttachmentCommand command) {
        attachmentService.bindForWorkOrder(command.getAttachmentIds(), command.getBizId(),
                command.getOwnerId(), command.getBizType());
        return R.ok();
    }

    /**
     * 附件元数据批量查询：供工单服务构造 AI 视觉核实请求。
     */
    @GetMapping("/attachments")
    public R<List<RectifyImageRef>> attachments(@RequestParam("ids") String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Long::valueOf)
                .toList();
        List<RectifyImageRef> result = attachmentService.listByIds(idList).stream()
                .map(vo -> new RectifyImageRef(vo.getId(), vo.getAccessUrl(), vo.getContentType(), vo.getFileSize()))
                .toList();
        return R.ok(result);
    }

    /**
     * 事件上报时绑定的原始照片：工单详情展示给管理员，并作为 AI 整改前后的对照。
     */
    @GetMapping("/{eventId}/attachments")
    public R<List<RectifyImageRef>> eventAttachments(@PathVariable("eventId") Long eventId) {
        List<RectifyImageRef> result = attachmentService.listByEvent(eventId).stream()
                .map(vo -> new RectifyImageRef(vo.getId(), vo.getAccessUrl(), vo.getContentType(), vo.getFileSize()))
                .toList();
        return R.ok(result);
    }

    /**
     * 工单被删除后事件退回待审核，由工单服务在删除事务内调用。
     */
    @PostMapping("/{eventId}/rollback-dispatch")
    public R<Void> rollbackDispatch(@PathVariable("eventId") Long eventId) {
        eventService.rollbackDispatch(eventId);
        return R.ok();
    }
}
