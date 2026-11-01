package com.city.platform.event.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.city.platform.common.api.dto.WorkOrderBrief;
import com.city.platform.common.core.model.PageResult;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.UserContext;
import com.city.platform.event.dto.AuditApproveRequest;
import com.city.platform.event.dto.AuditRejectRequest;
import com.city.platform.event.dto.CreateEventRequest;
import com.city.platform.event.dto.EventQuery;
import com.city.platform.event.dto.EventUpdateRequest;
import com.city.platform.event.dto.ResubmitEventRequest;
import com.city.platform.event.service.EventService;
import com.city.platform.event.vo.AreaStatVO;
import com.city.platform.event.vo.CategoryStatVO;
import com.city.platform.event.vo.EventDetailVO;
import com.city.platform.event.vo.EventListItemVO;
import com.city.platform.event.vo.TrendStatVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @SaCheckRole(RoleConstants.RESIDENT)
    public R<EventDetailVO> create(@Valid @RequestBody CreateEventRequest request) {
        return R.ok(eventService.create(request, UserContext.require()));
    }

    /**
     * 管理员代报：居民无法操作手机时由管理员在后台录入，流程与居民上报一致。
     */
    @PostMapping("/admin-report")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<EventDetailVO> adminReport(@Valid @RequestBody CreateEventRequest request) {
        return R.ok(eventService.adminReport(request, UserContext.require()));
    }

    /**
     * 管理员修改事件信息：仅待审核事件可编辑。
     */
    @PutMapping("/{id}")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<EventDetailVO> update(@PathVariable("id") Long id,
                                   @Valid @RequestBody EventUpdateRequest request) {
        return R.ok(eventService.updateEvent(id, request, UserContext.require()));
    }

    /**
     * 管理员删除事件：仅待审核、已驳回、已撤销的事件可删除。
     */
    @DeleteMapping("/{id}")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<Void> delete(@PathVariable("id") Long id) {
        eventService.deleteEvent(id, UserContext.require());
        return R.ok();
    }

    @GetMapping("/my")
    @SaCheckRole(RoleConstants.RESIDENT)
    public R<PageResult<EventListItemVO>> myEvents(@Valid EventQuery query) {
        return R.ok(eventService.page(query, UserContext.require()));
    }

    @GetMapping
    @SaCheckRole(RoleConstants.ADMIN)
    public R<PageResult<EventListItemVO>> page(@Valid EventQuery query) {
        return R.ok(eventService.page(query, UserContext.require()));
    }

    @GetMapping("/{id}")
    public R<EventDetailVO> detail(@PathVariable("id") Long id) {
        return R.ok(eventService.detail(id, UserContext.require()));
    }

    @GetMapping("/map-points")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<java.util.List<com.city.platform.event.vo.MapPointVO>> mapPoints(@Valid EventQuery query) {
        return R.ok(eventService.mapPoints(query));
    }

    @GetMapping("/statistics/overview")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<com.city.platform.event.vo.EventStatVO> statisticsOverview() {
        return R.ok(eventService.statisticsOverview());
    }

    @GetMapping("/statistics/by-category")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<List<CategoryStatVO>> statisticsByCategory(
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(eventService.statisticsByCategory(startTime, endTime));
    }

    @GetMapping("/statistics/by-area")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<List<AreaStatVO>> statisticsByArea(
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.ok(eventService.statisticsByArea(startTime, endTime));
    }

    @GetMapping("/statistics/trend")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<List<TrendStatVO>> statisticsTrend(
            @RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        return R.ok(eventService.statisticsTrend(days));
    }

    @PostMapping("/{id}/cancel")
    @SaCheckRole(RoleConstants.RESIDENT)
    public R<Void> cancel(@PathVariable("id") Long id) {
        eventService.cancel(id, UserContext.require());
        return R.ok();
    }

    @PutMapping("/{id}/resubmit")
    @SaCheckRole(RoleConstants.RESIDENT)
    public R<EventDetailVO> resubmit(@PathVariable("id") Long id,
                                     @Valid @RequestBody ResubmitEventRequest request) {
        return R.ok(eventService.resubmit(id, request, UserContext.require()));
    }

    @PostMapping("/{id}/approve")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<WorkOrderBrief> approve(@PathVariable("id") Long id,
                                     @Valid @RequestBody AuditApproveRequest request) {
        return R.ok(eventService.approve(id, request, UserContext.require()));
    }

    @PostMapping("/{id}/reject")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<Void> reject(@PathVariable("id") Long id,
                          @Valid @RequestBody AuditRejectRequest request) {
        eventService.reject(id, request, UserContext.require());
        return R.ok();
    }

    /**
     * 管理员手动重新评估事件紧急程度。
     */
    @PostMapping("/{id}/ai-review")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<EventDetailVO> aiReview(@PathVariable("id") Long id) {
        return R.ok(eventService.aiReview(id, UserContext.require()));
    }
}
