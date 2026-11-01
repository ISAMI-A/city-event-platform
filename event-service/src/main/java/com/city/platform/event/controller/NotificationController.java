package com.city.platform.event.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.UserContext;
import com.city.platform.event.dto.ForwardNoticeRequest;
import com.city.platform.event.dto.RemindRequest;
import com.city.platform.event.dto.TeamNoticeRequest;
import com.city.platform.event.entity.Notification;
import com.city.platform.event.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/my")
    public R<List<Notification>> my() {
        return R.ok(notificationService.myNotifications(UserContext.requireUserId()));
    }

    @GetMapping("/{id}")
    public R<Notification> detail(@PathVariable("id") Long id) {
        return R.ok(notificationService.detail(id, UserContext.requireUserId()));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(notificationService.unreadCount(UserContext.requireUserId()));
    }

    @PostMapping("/{id}/read")
    public R<Void> read(@PathVariable("id") Long id) {
        notificationService.markRead(id, UserContext.requireUserId());
        return R.ok();
    }

    /**
     * 管理员提醒责任组长跟进居民评价。
     */
    @PostMapping("/remind")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<Void> remind(@Valid @RequestBody RemindRequest request) {
        notificationService.remind(request, UserContext.require());
        return R.ok();
    }

    /**
     * 组长把居民反馈转达给本组组员。
     */
    @PostMapping("/forward")
    @SaCheckRole(RoleConstants.WORKER)
    public R<Void> forward(@Valid @RequestBody ForwardNoticeRequest request) {
        notificationService.forward(request, UserContext.require());
        return R.ok();
    }

    /**
     * 组长向本组组员发布注意事项。
     */
    @PostMapping("/team-notice")
    @SaCheckRole(RoleConstants.WORKER)
    public R<Integer> teamNotice(@Valid @RequestBody TeamNoticeRequest request) {
        return R.ok(notificationService.teamNotice(request, UserContext.require()));
    }
}
