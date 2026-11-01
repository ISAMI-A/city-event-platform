package com.city.platform.event.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.platform.common.api.dto.AiAnalysisResult;
import com.city.platform.common.api.dto.EventReviewRequest;
import com.city.platform.common.api.dto.EventReviewResult;
import com.city.platform.common.api.dto.GeoResult;
import com.city.platform.common.api.dto.WorkOrderBrief;
import com.city.platform.common.api.dto.WorkerProfile;
import com.city.platform.common.api.feign.AiClient;
import com.city.platform.common.api.feign.MapClient;
import com.city.platform.common.api.feign.WorkOrderClient;
import com.city.platform.common.api.feign.UserClient;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.PageResult;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.LoginUser;
import com.city.platform.event.constant.EventStatus;
import com.city.platform.event.constant.FuzhouArea;
import com.city.platform.event.dto.AuditApproveRequest;
import com.city.platform.event.dto.AuditRejectRequest;
import com.city.platform.event.dto.CreateEventRequest;
import com.city.platform.event.dto.DateCount;
import com.city.platform.event.dto.EventQuery;
import com.city.platform.event.dto.EventUpdateRequest;
import com.city.platform.event.dto.ResubmitEventRequest;
import com.city.platform.event.entity.Event;
import com.city.platform.event.entity.EventAiSnapshot;
import com.city.platform.event.entity.EventAuditLog;
import com.city.platform.event.entity.EventStatusLog;
import com.city.platform.event.mapper.EventAiSnapshotMapper;
import com.city.platform.event.mapper.EventAuditLogMapper;
import com.city.platform.event.mapper.EventMapper;
import com.city.platform.event.mapper.EventStatusLogMapper;
import com.city.platform.event.vo.AiSnapshotVO;
import com.city.platform.event.vo.AreaStatVO;
import com.city.platform.event.vo.AttachmentVO;
import com.city.platform.event.vo.CategoryStatVO;
import com.city.platform.event.vo.EventDetailVO;
import com.city.platform.event.vo.EventListItemVO;
import com.city.platform.event.vo.TimelineItemVO;
import com.city.platform.event.vo.TrendStatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventMapper eventMapper;
    private final EventStatusLogMapper statusLogMapper;
    private final EventAuditLogMapper auditLogMapper;
    private final EventAiSnapshotMapper aiSnapshotMapper;
    private final AttachmentService attachmentService;
    private final EventNoGenerator eventNoGenerator;
    private final MapClient mapClient;
    private final AiClient aiClient;
    private final WorkOrderClient workOrderClient;
    private final UserClient userClient;
    private final TransactionTemplate transactionTemplate;
    private final EventCacheService eventCacheService;
    private final EventDispatchService eventDispatchService;
    private final AutoDispatchService autoDispatchService;

    /**
     * 创建事件：外部调用（地图/AI）在事务外完成，数据库写入使用短事务。
     */
    public EventDetailVO create(CreateEventRequest request, LoginUser user) {
        if (!RoleConstants.RESIDENT.equals(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅居民可以上报事件");
        }
        return createInternal(request, user.getUserId(), user.getUserId(), user.getRole(), "居民提交事件");
    }

    /**
     * 管理员代报：上报人缺省为管理员本人，也可指定居民，其余校验与居民上报一致。
     */
    public EventDetailVO adminReport(CreateEventRequest request, LoginUser admin) {
        if (!RoleConstants.ADMIN.equals(admin.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可以代报事件");
        }
        Long reporterId = request.getReporterId() != null ? request.getReporterId() : admin.getUserId();
        return createInternal(request, reporterId, admin.getUserId(), admin.getRole(), "管理员代报事件");
    }

    private EventDetailVO createInternal(CreateEventRequest request, Long reporterId, Long operatorId,
                                         String operatorRole, String remark) {
        request.setReporterId(null);
        validateEnums(request.getCategoryCode(), request.getPriority());
        List<Long> attachmentIds = request.getAttachmentIds() == null ? List.of() : request.getAttachmentIds();
        GeoResult geo = fetchGeo(request);
        if (!StringUtils.hasText(geo.getAreaCode())) {
            // 逆地址解析失败时用地址文本兜底识别片区，保证后续能匹配组长
            String text = StringUtils.hasText(geo.getAddress()) ? geo.getAddress() : request.getAddress();
            String resolved = FuzhouArea.resolveByText(text);
            if (resolved != null) {
                geo.setAreaCode(resolved);
                if (!StringUtils.hasText(geo.getDistrict())) {
                    geo.setDistrict(FuzhouArea.DISTRICTS.get(resolved));
                }
            }
        }
        validateFuzhouScope(request.getLongitude(), request.getLatitude(),
                StringUtils.hasText(geo.getAreaCode()) ? geo.getAreaCode() : request.getAreaCode());
        AiAnalysisResult ai = fetchAi(request.getAiCallId());

        EventDetailVO detail = transactionTemplate.execute(status -> {
            Event event = new Event();
            event.setEventNo(eventNoGenerator.next());
            event.setReporterId(reporterId);
            event.setCategoryCode(request.getCategoryCode());
            event.setPriority(request.getPriority());
            event.setTitle(buildTitle(request));
            event.setDescription(request.getDescription());
            event.setStatus(EventStatus.PENDING_REVIEW);
            event.setLongitude(request.getLongitude());
            event.setLatitude(request.getLatitude());
            event.setAddress(StringUtils.hasText(geo.getAddress()) ? geo.getAddress() : request.getAddress());
            event.setProvince(geo.getProvince());
            event.setCity(geo.getCity());
            event.setDistrict(geo.getDistrict());
            event.setStreet(geo.getStreet());
            event.setAreaCode(StringUtils.hasText(geo.getAreaCode()) ? geo.getAreaCode() : request.getAreaCode());
            event.setPhotoCount(attachmentIds.size());
            if (ai != null) {
                event.setAiCategoryCode(ai.getCategoryCode());
                event.setAiPriority(ai.getPriority());
                event.setAiConfidence(ai.getConfidence());
                event.setAiSuggestion(ai.getSuggestion());
                event.setSummary(ai.getSummary());
            }
            eventMapper.insert(event);

            if (!attachmentIds.isEmpty()) {
                attachmentService.bind(attachmentIds, event.getId(), operatorId, "EVENT_REPORT");
            }
            saveStatusLog(event, null, EventStatus.PENDING_REVIEW, "CREATE",
                    operatorId, operatorRole, remark);
            saveAiSnapshot(event.getId(), ai, request);

            log.info("事件创建成功 eventId={} eventNo={} reporterId={} operator={} geoPending={}",
                    event.getId(), event.getEventNo(), reporterId, operatorId, geo.isPending());
            return buildDetail(event);
        });
        eventCacheService.evictStat();
        autoDispatchService.tryDispatch(detail.getId());
        aiReviewAsync(detail.getId());
        // 自动派单可能已改变状态，返回派单后的最新详情
        return buildDetail(eventMapper.selectById(detail.getId()));
    }

    /**
     * 管理员修改事件基础信息：仅待审核状态可改，写入操作流水。
     */
    public EventDetailVO updateEvent(Long eventId, EventUpdateRequest request, LoginUser admin) {
        Event updated = transactionTemplate.execute(status -> {
            Event event = eventMapper.selectById(eventId);
            if (event == null) {
                throw new BizException(ErrorCode.EVENT_NOT_FOUND);
            }
            if (!EventStatus.PENDING_REVIEW.equals(event.getStatus())) {
                throw new BizException(ErrorCode.EVENT_STATE_NOT_ALLOWED, "仅待审核事件可编辑");
            }
            String categoryCode = StringUtils.hasText(request.getCategoryCode())
                    ? request.getCategoryCode() : event.getCategoryCode();
            String priority = StringUtils.hasText(request.getPriority())
                    ? request.getPriority() : event.getPriority();
            validateEnums(categoryCode, priority);

            Event update = new Event();
            update.setId(eventId);
            update.setTitle(request.getTitle());
            update.setDescription(request.getDescription());
            update.setCategoryCode(request.getCategoryCode());
            update.setPriority(request.getPriority());
            update.setAddress(request.getAddress());
            update.setAreaCode(request.getAreaCode());
            if (StringUtils.hasText(request.getDescription()) && !StringUtils.hasText(request.getTitle())) {
                update.setTitle(buildTitle(null, request.getDescription()));
            }
            eventMapper.updateById(update);

            saveStatusLog(event, EventStatus.PENDING_REVIEW, EventStatus.PENDING_REVIEW, "EDIT",
                    admin.getUserId(), admin.getRole(), "管理员修改事件信息");
            log.info("管理员修改事件 eventId={} adminId={}", eventId, admin.getUserId());
            return eventMapper.selectById(eventId);
        });
        eventCacheService.evictAfterChange(eventId);
        return buildDetail(updated);
    }

    /**
     * 管理员删除事件：仅未进入派单流程的事件可删除（逻辑删除）。
     */
    public void deleteEvent(Long eventId, LoginUser admin) {
        transactionTemplate.executeWithoutResult(status -> {
            Event event = eventMapper.selectById(eventId);
            if (event == null) {
                throw new BizException(ErrorCode.EVENT_NOT_FOUND);
            }
            if (!EventStatus.PENDING_REVIEW.equals(event.getStatus())
                    && !EventStatus.REJECTED.equals(event.getStatus())
                    && !EventStatus.CANCELLED.equals(event.getStatus())) {
                throw new BizException(ErrorCode.EVENT_STATE_NOT_ALLOWED, "仅待审核、已驳回、已撤销的事件可删除");
            }
            eventMapper.deleteById(eventId);
            saveStatusLog(event, event.getStatus(), event.getStatus(), "DELETE",
                    admin.getUserId(), admin.getRole(), "管理员删除事件");
            log.info("管理员删除事件 eventId={} eventNo={} adminId={}", eventId, event.getEventNo(), admin.getUserId());
        });
        eventCacheService.evictAfterChange(eventId);
        eventCacheService.evictStat();
    }

    /**
     * 工单删除后回滚：已派单事件退回待审核，再次进入人工派单队列。
     */
    public void rollbackDispatch(Long eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            Event event = eventMapper.selectById(eventId);
            if (event == null) {
                throw new BizException(ErrorCode.EVENT_NOT_FOUND);
            }
            if (EventStatus.PENDING_REVIEW.equals(event.getStatus())) {
                return;
            }
            EventStateMachine.assertTransit(event.getStatus(), EventStatus.PENDING_REVIEW);
            int rows = eventMapper.updateStatus(eventId, EventStatus.DISPATCHED, EventStatus.PENDING_REVIEW);
            if (rows == 0) {
                throw new BizException(ErrorCode.DUPLICATE_OPERATION, "事件状态已变化，请刷新后重试");
            }
            saveStatusLog(event, EventStatus.DISPATCHED, EventStatus.PENDING_REVIEW, "WO_DELETE",
                    0L, "SYSTEM", "删除工单，事件退回待审核");
            log.info("工单删除回滚事件 eventId={}", eventId);
        });
        eventCacheService.evictAfterChange(eventId);
        eventCacheService.evictStat();
    }

    /**
     * 居民撤销上报：仅本人、仅待审核，条件更新防并发。
     */
    public void cancel(Long eventId, LoginUser user) {
        transactionTemplate.executeWithoutResult(status -> {
            Event event = eventMapper.selectById(eventId);
            if (event == null) {
                throw new BizException(ErrorCode.EVENT_NOT_FOUND);
            }
            if (!event.getReporterId().equals(user.getUserId())) {
                throw new BizException(ErrorCode.RESOURCE_NOT_OWNED, "无权撤销他人事件");
            }
            EventStateMachine.assertTransit(event.getStatus(), EventStatus.CANCELLED);
            int rows = eventMapper.updateStatus(eventId, EventStatus.PENDING_REVIEW, EventStatus.CANCELLED);
            if (rows == 0) {
                throw new BizException(ErrorCode.DUPLICATE_OPERATION, "事件已被处理，请刷新后重试");
            }
            saveStatusLog(event, EventStatus.PENDING_REVIEW, EventStatus.CANCELLED, "CANCEL",
                    user.getUserId(), user.getRole(), "居民撤销上报");
            log.info("居民撤销事件 eventId={} reporterId={}", eventId, user.getUserId());
        });
        eventCacheService.evictAfterChange(eventId);
    }

    /**
     * 居民修改后重新提交：驳回态回到待审核，先条件更新状态再更新内容。
     */
    public EventDetailVO resubmit(Long eventId, ResubmitEventRequest request, LoginUser user) {
        validateEnums(request.getCategoryCode(), request.getPriority());
        EventDetailVO detail = transactionTemplate.execute(status -> {
            Event event = eventMapper.selectById(eventId);
            if (event == null) {
                throw new BizException(ErrorCode.EVENT_NOT_FOUND);
            }
            if (!event.getReporterId().equals(user.getUserId())) {
                throw new BizException(ErrorCode.RESOURCE_NOT_OWNED, "无权修改他人事件");
            }
            EventStateMachine.assertTransit(event.getStatus(), EventStatus.PENDING_REVIEW);
            validateFuzhouScope(
                    request.getLongitude() != null ? request.getLongitude() : event.getLongitude(),
                    request.getLatitude() != null ? request.getLatitude() : event.getLatitude(),
                    StringUtils.hasText(request.getAreaCode()) ? request.getAreaCode() : event.getAreaCode());

            int rows = eventMapper.updateStatus(eventId, EventStatus.REJECTED, EventStatus.PENDING_REVIEW);
            if (rows == 0) {
                throw new BizException(ErrorCode.DUPLICATE_OPERATION, "事件已被处理，请刷新后重试");
            }

            boolean hasNewAttachment = request.getAttachmentIds() != null && !request.getAttachmentIds().isEmpty();
            if (hasNewAttachment) {
                attachmentService.bind(request.getAttachmentIds(), eventId, user.getUserId(), "EVENT_REPORT");
            }

            Event update = new Event();
            update.setDescription(request.getDescription());
            update.setTitle(buildTitle(null, request.getDescription()));
            update.setCategoryCode(request.getCategoryCode());
            update.setPriority(request.getPriority());
            if (request.getLongitude() != null) {
                update.setLongitude(request.getLongitude());
            }
            if (request.getLatitude() != null) {
                update.setLatitude(request.getLatitude());
            }
            if (StringUtils.hasText(request.getAddress())) {
                update.setAddress(request.getAddress());
            }
            if (StringUtils.hasText(request.getAreaCode())) {
                update.setAreaCode(request.getAreaCode());
            }
            if (hasNewAttachment) {
                update.setPhotoCount(attachmentService.countByEvent(eventId));
            }
            eventMapper.update(update, Wrappers.<Event>lambdaUpdate()
                    .eq(Event::getId, eventId)
                    .set(Event::getRejectReason, null)
                    .setSql("version = version + 1"));

            saveStatusLog(event, EventStatus.REJECTED, EventStatus.PENDING_REVIEW, "RESUBMIT",
                    user.getUserId(), user.getRole(), "居民修改后重新提交");
            log.info("居民重新提交事件 eventId={} reporterId={} newPhotos={}",
                    eventId, user.getUserId(), hasNewAttachment ? request.getAttachmentIds().size() : 0);
            return buildDetail(eventMapper.selectById(eventId));
        });
        eventCacheService.evictAfterChange(eventId);
        autoDispatchService.tryDispatch(eventId);
        aiReviewAsync(eventId);
        return detail;
    }

    public PageResult<EventListItemVO> page(EventQuery query, LoginUser user) {
        if (RoleConstants.WORKER.equals(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "处理人员请通过工单查看事件");
        }
        var wrapper = Wrappers.<Event>lambdaQuery();
        if (RoleConstants.RESIDENT.equals(user.getRole())) {
            wrapper.eq(Event::getReporterId, user.getUserId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Event::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getCategoryCode())) {
            wrapper.eq(Event::getCategoryCode, query.getCategoryCode());
        }
        if (StringUtils.hasText(query.getAreaCode())) {
            wrapper.eq(Event::getAreaCode, query.getAreaCode());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Event::getTitle, query.getKeyword())
                    .or().like(Event::getDescription, query.getKeyword())
                    .or().like(Event::getEventNo, query.getKeyword()));
        }
        if (query.getStartTime() != null) {
            wrapper.ge(Event::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(Event::getCreateTime, query.getEndTime());
        }
        // 优先级按紧急程度排序：URGENT > HIGH > MEDIUM > LOW；管理员默认待办置顶
        if ("priority".equalsIgnoreCase(query.getSortBy())) {
            String direction = "asc".equalsIgnoreCase(query.getOrder()) ? "ASC" : "DESC";
            wrapper.last("ORDER BY FIELD(priority, 'LOW','MEDIUM','HIGH','URGENT') " + direction
                    + ", create_time DESC");
        } else if (RoleConstants.ADMIN.equals(user.getRole())) {
            wrapper.last("ORDER BY CASE WHEN status = 'PENDING_REVIEW' THEN 0 ELSE 1 END, create_time DESC");
        } else {
            wrapper.orderByDesc(Event::getCreateTime);
        }
        Page<Event> page = eventMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        var voPage = page.convert(this::toListItem);
        fillWorkOrderInfo(voPage.getRecords());
        return PageResult.of(voPage);
    }

    public EventDetailVO detail(Long eventId, LoginUser user) {
        EventDetailVO cached = eventCacheService.getDetail(eventId);
        if (cached != null) {
            assertCanView(cached.getReporterId(), user);
            return cached;
        }
        if (eventCacheService.isNullCached(eventId)) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            eventCacheService.putNull(eventId);
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        assertCanView(event.getReporterId(), user);
        EventDetailVO detail = buildDetail(event);
        eventCacheService.putDetail(eventId, detail);
        return detail;
    }

    private void assertCanView(Long reporterId, LoginUser user) {
        if (RoleConstants.WORKER.equals(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "处理人员请通过工单查看事件");
        }
        if (RoleConstants.RESIDENT.equals(user.getRole())
                && !reporterId.equals(user.getUserId())) {
            throw new BizException(ErrorCode.RESOURCE_NOT_OWNED, "无权查看他人事件");
        }
    }

    public com.city.platform.event.vo.EventStatVO statisticsOverview() {
        com.city.platform.event.vo.EventStatVO cached = eventCacheService.getStat();
        if (cached != null) {
            return cached;
        }
        com.city.platform.event.vo.EventStatVO stat = new com.city.platform.event.vo.EventStatVO();
        long total = 0;
        for (com.city.platform.event.dto.StatusCount item : eventMapper.countByStatus()) {
            long count = item.getCnt() == null ? 0 : item.getCnt();
            total += count;
            switch (item.getStatus()) {
                case EventStatus.PENDING_REVIEW -> stat.setPendingReview(count);
                case EventStatus.DISPATCHED -> stat.setDispatched(count);
                case EventStatus.PROCESSING -> stat.setProcessing(count);
                case EventStatus.PENDING_ACCEPTANCE -> stat.setPendingAcceptance(count);
                case EventStatus.CLOSED -> stat.setClosed(count);
                case EventStatus.REJECTED -> stat.setRejected(count);
                case EventStatus.CANCELLED -> stat.setCancelled(count);
                default -> {
                }
            }
        }
        stat.setTotal(total);
        eventCacheService.putStat(stat);
        return stat;
    }

    public List<CategoryStatVO> statisticsByCategory(LocalDateTime startTime, LocalDateTime endTime) {
        return eventMapper.countByCategory(startTime, endTime).stream().map(item -> {
            CategoryStatVO vo = new CategoryStatVO();
            vo.setCode(item.getCode());
            vo.setName(EventStatus.CATEGORY_NAMES.getOrDefault(item.getCode(), item.getCode()));
            vo.setCount(item.getCnt() == null ? 0 : item.getCnt());
            return vo;
        }).toList();
    }

    public List<AreaStatVO> statisticsByArea(LocalDateTime startTime, LocalDateTime endTime) {
        return eventMapper.countByArea(startTime, endTime).stream().map(item -> {
            AreaStatVO vo = new AreaStatVO();
            vo.setAreaCode(item.getAreaCode());
            vo.setAreaName(StringUtils.hasText(item.getAreaName())
                    ? item.getAreaName()
                    : (StringUtils.hasText(item.getAreaCode()) ? item.getAreaCode() : "未划分区域"));
            vo.setCount(item.getCnt() == null ? 0 : item.getCnt());
            return vo;
        }).toList();
    }

    /**
     * 最近 N 天趋势：缺失日期补 0，按日期升序。
     */
    public List<TrendStatVO> statisticsTrend(Integer days) {
        int span = days == null ? 7 : Math.max(1, Math.min(days, 30));
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(span - 1L);
        Map<String, Long> counts = eventMapper.countByDate(startDate.atStartOfDay(),
                        today.plusDays(1).atStartOfDay())
                .stream()
                .collect(Collectors.toMap(DateCount::getDate, item -> item.getCnt() == null ? 0L : item.getCnt()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<TrendStatVO> trend = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            String date = startDate.plusDays(i).format(formatter);
            TrendStatVO vo = new TrendStatVO();
            vo.setDate(date);
            vo.setCount(counts.getOrDefault(date, 0L));
            trend.add(vo);
        }
        return trend;
    }

    /**
     * 审核通过并派单：交由 {@link EventDispatchService} 在 Seata 全局事务中执行，
     * 覆盖事件状态更新与工单创建两个库的写操作，故障时整体回滚。
     */
    public WorkOrderBrief approve(Long eventId, AuditApproveRequest request, LoginUser admin) {
        validateEnums(request.getCategoryCode(), request.getPriority());
        WorkerProfile worker = fetchWorker(request.getWorkerId());
        if (!"LEADER".equals(worker.getWorkerType())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "只能派单给组长，组员由组长分配");
        }
        return eventDispatchService.dispatch(eventId, worker, request.getCategoryCode(), request.getPriority(),
                request.getAreaCode(), admin.getUserId(), admin.getRole(),
                "APPROVE", "APPROVE", request.getRemark());
    }

    public void reject(Long eventId, AuditRejectRequest request, LoginUser admin) {
        transactionTemplate.executeWithoutResult(status -> {
            Event event = eventMapper.selectById(eventId);
            if (event == null) {
                throw new BizException(ErrorCode.EVENT_NOT_FOUND);
            }
            EventStateMachine.assertTransit(event.getStatus(), EventStatus.REJECTED);
            int rows = eventMapper.reject(eventId, admin.getUserId(), request.getReason());
            if (rows == 0) {
                throw new BizException(ErrorCode.DUPLICATE_OPERATION, "事件已被处理，请刷新后重试");
            }
            EventAuditLog auditLog = new EventAuditLog();
            auditLog.setEventId(eventId);
            auditLog.setAuditorId(admin.getUserId());
            auditLog.setDecision("REJECT");
            auditLog.setBeforeCategory(event.getCategoryCode());
            auditLog.setAfterCategory(event.getCategoryCode());
            auditLog.setBeforePriority(event.getPriority());
            auditLog.setAfterPriority(event.getPriority());
            auditLog.setReason(request.getReason());
            auditLogMapper.insert(auditLog);
            saveStatusLog(event, EventStatus.PENDING_REVIEW, EventStatus.REJECTED, "REJECT",
                    admin.getUserId(), admin.getRole(), request.getReason());
            log.info("事件驳回 eventId={} auditorId={} reason={}", eventId, admin.getUserId(), request.getReason());
        });
        eventCacheService.evictAfterChange(eventId);
    }

    /**
     * 上报后自动异步复评紧急程度：失败只记日志，不影响居民上报。
     */
    public void aiReviewAsync(Long eventId) {
        CompletableFuture.runAsync(() -> {
            try {
                doAiReview(eventId);
            } catch (Exception e) {
                log.warn("事件 AI 紧急度复评失败（不影响上报） eventId={}: {}", eventId, e.getMessage());
            }
        });
    }

    /**
     * 管理员手动重新评估紧急程度，覆盖上一次复评结果。
     */
    public EventDetailVO aiReview(Long eventId, LoginUser admin) {
        doAiReview(eventId);
        eventCacheService.evictAfterChange(eventId);
        return detail(eventId, admin);
    }

    private void doAiReview(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            return;
        }
        EventReviewRequest request = new EventReviewRequest(event.getTitle(), event.getDescription(),
                event.getAddress(), event.getCategoryCode(), event.getPriority(), event.getPhotoCount());
        EventReviewResult result;
        try {
            R<EventReviewResult> response = aiClient.reviewEvent(request);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.warn("事件紧急度复评无结果 eventId={} message={}", eventId,
                        response == null ? "无响应" : response.getMessage());
                return;
            }
            result = response.getData();
        } catch (Exception e) {
            log.warn("事件紧急度复评调用失败 eventId={}: {}", eventId, e.getMessage());
            return;
        }
        if (result.isDegraded()) {
            log.warn("事件紧急度复评降级 eventId={} reason={}", eventId, result.getDegradeReason());
            return;
        }
        Event update = new Event();
        update.setId(eventId);
        update.setAiReviewPriority(result.getPriority());
        update.setAiReviewReason(result.getReason());
        update.setAiReviewImpact(result.getImpact());
        update.setAiReviewResponseMinutes(result.getResponseMinutes());
        update.setAiReviewConfidence(result.getConfidence());
        update.setAiReviewTime(LocalDateTime.now());
        eventMapper.updateById(update);
        eventCacheService.evictAfterChange(eventId);
        autoDispatchService.tryDispatch(eventId);
        log.info("事件紧急度复评完成 eventId={} residentPriority={} aiPriority={}",
                eventId, event.getPriority(), result.getPriority());
    }

    private void validateFuzhouScope(BigDecimal longitude, BigDecimal latitude, String areaCode) {
        String message = "仅支持福州市六城区（鼓楼区、台江区、仓山区、马尾区、晋安区、长乐区）范围内上报事件";
        if (!FuzhouArea.inBounds(longitude, latitude)) {
            throw new BizException(ErrorCode.PARAM_INVALID, message);
        }
        if (StringUtils.hasText(areaCode) && !FuzhouArea.isDistrict(areaCode)) {
            throw new BizException(ErrorCode.PARAM_INVALID, message);
        }
    }

    public java.util.List<com.city.platform.event.vo.MapPointVO> mapPoints(EventQuery query) {
        var wrapper = Wrappers.<Event>lambdaQuery()
                .select(Event::getId, Event::getEventNo, Event::getLongitude, Event::getLatitude,
                        Event::getCategoryCode, Event::getStatus, Event::getPriority, Event::getAddress);
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Event::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getCategoryCode())) {
            wrapper.eq(Event::getCategoryCode, query.getCategoryCode());
        }
        if (StringUtils.hasText(query.getAreaCode())) {
            wrapper.eq(Event::getAreaCode, query.getAreaCode());
        }
        wrapper.orderByDesc(Event::getCreateTime).last("LIMIT 500");
        return eventMapper.selectList(wrapper).stream().map(event -> {
            com.city.platform.event.vo.MapPointVO vo = new com.city.platform.event.vo.MapPointVO();
            BeanUtils.copyProperties(event, vo);
            return vo;
        }).toList();
    }

    public com.city.platform.common.api.dto.EventSnapshot snapshot(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        return new com.city.platform.common.api.dto.EventSnapshot(
                event.getId(), event.getEventNo(), event.getTitle(), event.getDescription(),
                event.getCategoryCode(), event.getPriority(), event.getStatus(),
                event.getAddress(), event.getLongitude(), event.getLatitude(),
                event.getReporterId(), event.getPhotoCount());
    }

    private GeoResult fetchGeo(CreateEventRequest request) {
        try {
            R<GeoResult> response = mapClient.reverseGeocode(
                    request.getLongitude(), request.getLatitude(), request.getAddress());
            if (response != null && response.isSuccess() && response.getData() != null) {
                return response.getData();
            }
            log.warn("地图解析返回异常，使用用户填写地址 message={}",
                    response == null ? "无响应" : response.getMessage());
        } catch (Exception e) {
            log.warn("地图解析调用失败，降级使用用户填写地址: {}", e.getMessage());
        }
        GeoResult fallback = new GeoResult();
        fallback.setAddress(request.getAddress());
        fallback.setAreaCode(request.getAreaCode());
        fallback.setPending(true);
        return fallback;
    }

    private AiAnalysisResult fetchAi(Long aiCallId) {
        if (aiCallId == null) {
            return null;
        }
        try {
            R<AiAnalysisResult> response = aiClient.getAnalysis(aiCallId);
            if (response != null && response.isSuccess()) {
                return response.getData();
            }
            log.warn("AI 分析结果不可用 callId={}", aiCallId);
        } catch (Exception e) {
            log.warn("AI 分析结果查询失败 callId={}: {}", aiCallId, e.getMessage());
        }
        return null;
    }

    private WorkerProfile fetchWorker(Long workerId) {
        try {
            R<WorkerProfile> response = userClient.workerProfile(workerId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BizException(ErrorCode.PARAM_INVALID,
                        response == null ? "处理人员校验失败" : response.getMessage());
            }
            return response.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "处理人员服务不可用：" + e.getMessage());
        }
    }

    private void validateEnums(String categoryCode, String priority) {
        if (!EventStatus.CATEGORIES.contains(categoryCode)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "事件类别不合法");
        }
        if (!EventStatus.PRIORITIES.contains(priority)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "优先级不合法");
        }
    }

    private String buildTitle(CreateEventRequest request) {
        return buildTitle(request.getTitle(), request.getDescription());
    }

    private String buildTitle(String title, String description) {
        if (StringUtils.hasText(title)) {
            return title;
        }
        return description.length() > 30 ? description.substring(0, 30) : description;
    }

    private void saveStatusLog(Event event, String from, String to, String action,
                               Long operatorId, String role, String remark) {
        EventStatusLog statusLog = new EventStatusLog();
        statusLog.setEventId(event.getId());
        statusLog.setEventNo(event.getEventNo());
        statusLog.setFromStatus(from);
        statusLog.setToStatus(to);
        statusLog.setAction(action);
        statusLog.setOperatorId(operatorId);
        statusLog.setOperatorRole(role);
        statusLog.setRemark(remark);
        statusLogMapper.insert(statusLog);
    }

    private void saveAiSnapshot(Long eventId, AiAnalysisResult ai, CreateEventRequest request) {
        if (ai == null) {
            return;
        }
        EventAiSnapshot snapshot = new EventAiSnapshot();
        snapshot.setEventId(eventId);
        snapshot.setAiCallLogId(ai.getCallId());
        snapshot.setCategoryCode(ai.getCategoryCode());
        snapshot.setPriority(ai.getPriority());
        snapshot.setSummary(ai.getSummary());
        snapshot.setSuggestion(ai.getSuggestion());
        snapshot.setConfidence(ai.getConfidence());
        snapshot.setModelName(ai.getModel());
        boolean accepted = ai.getCategoryCode().equals(request.getCategoryCode())
                && ai.getPriority().equals(request.getPriority());
        snapshot.setAccepted(accepted ? 1 : 0);
        aiSnapshotMapper.insert(snapshot);
    }

    private EventListItemVO toListItem(Event event) {
        EventListItemVO vo = new EventListItemVO();
        BeanUtils.copyProperties(event, vo);
        vo.setPending(EventStatus.PENDING_REVIEW.equals(event.getStatus()));
        return vo;
    }

    /**
     * 批量补齐工单、责任组长与居民评价：跨库数据由工单服务提供，失败不阻断列表。
     */
    private void fillWorkOrderInfo(List<EventListItemVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        String eventIds = records.stream().map(item -> String.valueOf(item.getId()))
                .collect(java.util.stream.Collectors.joining(","));
        try {
            R<List<com.city.platform.common.api.dto.EventRating>> response = workOrderClient.ratings(eventIds);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                return;
            }
            java.util.Map<Long, com.city.platform.common.api.dto.EventRating> indexed =
                    response.getData().stream().collect(java.util.stream.Collectors.toMap(
                            com.city.platform.common.api.dto.EventRating::getEventId, item -> item, (a, b) -> a));
            records.forEach(vo -> {
                com.city.platform.common.api.dto.EventRating info = indexed.get(vo.getId());
                if (info == null) {
                    return;
                }
                vo.setWorkOrderId(info.getWorkOrderId());
                vo.setWorkOrderStatus(info.getWorkOrderStatus());
                vo.setLeaderId(info.getLeaderId());
                vo.setLeaderName(info.getLeaderName());
                vo.setAssigneeName(info.getAssigneeName());
                vo.setRating(info.getRating());
                vo.setRatingTags(info.getTags());
                vo.setRatingComment(info.getComment());
            });
        } catch (Exception e) {
            log.warn("批量查询事件评价失败（不影响列表）: {}", e.getMessage());
        }
    }

    private EventDetailVO buildDetail(Event event) {
        EventDetailVO vo = new EventDetailVO();
        BeanUtils.copyProperties(event, vo);
        List<AttachmentVO> attachments = attachmentService.listByEvent(event.getId());
        vo.setAttachments(attachments);
        List<TimelineItemVO> timeline = statusLogMapper.selectList(Wrappers.<EventStatusLog>lambdaQuery()
                        .eq(EventStatusLog::getEventId, event.getId())
                        .orderByAsc(EventStatusLog::getId))
                .stream().map(item -> {
                    TimelineItemVO timelineItem = new TimelineItemVO();
                    BeanUtils.copyProperties(item, timelineItem);
                    return timelineItem;
                }).toList();
        vo.setTimeline(timeline);

        EventAiSnapshot snapshot = aiSnapshotMapper.selectOne(Wrappers.<EventAiSnapshot>lambdaQuery()
                .eq(EventAiSnapshot::getEventId, event.getId()).last("LIMIT 1"));
        if (snapshot != null) {
            AiSnapshotVO aiSnapshot = new AiSnapshotVO();
            BeanUtils.copyProperties(snapshot, aiSnapshot);
            vo.setAiSnapshot(aiSnapshot);
        }
        try {
            R<WorkOrderBrief> response = workOrderClient.getByEvent(event.getId());
            if (response != null && response.isSuccess()) {
                vo.setWorkOrder(response.getData());
            }
        } catch (Exception e) {
            log.warn("查询工单摘要失败（不影响事件详情） eventId={}: {}", event.getId(), e.getMessage());
        }
        return vo;
    }
}
