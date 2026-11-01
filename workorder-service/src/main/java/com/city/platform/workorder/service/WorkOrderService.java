package com.city.platform.workorder.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.platform.common.api.dto.CreateWorkOrderCommand;
import com.city.platform.common.api.dto.EventSnapshot;
import com.city.platform.common.api.dto.RectifyImageRef;
import com.city.platform.common.api.dto.WorkOrderBrief;
import com.city.platform.common.api.dto.WorkerProfile;
import com.city.platform.common.api.feign.EventClient;
import com.city.platform.common.api.feign.UserClient;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.PageResult;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.LoginUser;
import com.city.platform.workorder.constant.WorkOrderStatus;
import com.city.platform.workorder.dto.AcceptanceRequest;
import com.city.platform.workorder.dto.AssignRequest;
import com.city.platform.workorder.dto.LeaderReviewRequest;
import com.city.platform.workorder.dto.ProcessRequest;
import com.city.platform.workorder.dto.RatingRequest;
import com.city.platform.workorder.dto.ReassignRequest;
import com.city.platform.workorder.dto.SubmitRequest;
import com.city.platform.workorder.dto.WorkOrderQuery;
import com.city.platform.workorder.entity.WorkOrder;
import com.city.platform.workorder.entity.WorkOrderAcceptance;
import com.city.platform.workorder.entity.WorkOrderLog;
import com.city.platform.workorder.entity.WorkOrderReassign;
import com.city.platform.workorder.entity.WorkRating;
import com.city.platform.workorder.mapper.WorkOrderAcceptanceMapper;
import com.city.platform.workorder.mapper.WorkOrderLogMapper;
import com.city.platform.workorder.mapper.WorkOrderMapper;
import com.city.platform.workorder.mapper.WorkOrderReassignMapper;
import com.city.platform.workorder.mapper.WorkRatingMapper;
import com.city.platform.workorder.mq.WorkOrderEventPublisher;
import com.city.platform.workorder.vo.WorkOrderAcceptanceVO;
import com.city.platform.workorder.vo.WorkOrderDetailVO;
import com.city.platform.workorder.vo.WorkOrderListItemVO;
import com.city.platform.workorder.vo.WorkOrderLogVO;
import com.city.platform.workorder.vo.WorkRatingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;
    private final WorkOrderAcceptanceMapper acceptanceMapper;
    private final WorkOrderReassignMapper reassignMapper;
    private final WorkRatingMapper ratingMapper;
    private final WorkOrderNoGenerator noGenerator;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final WorkOrderEventPublisher publisher;
    private final TransactionTemplate transactionTemplate;
    private final Environment environment;
    private final AiVerifyService aiVerifyService;

    /**
     * 内部创建工单：按 event_id 幂等，重复调用返回已有工单。
     */
    @Transactional
    public WorkOrderBrief createInternal(CreateWorkOrderCommand command) {
        WorkOrder existing = findByEventId(command.getEventId());
        if (existing != null) {
            log.info("工单已存在，幂等返回 eventId={} workOrderNo={}", command.getEventId(), existing.getWorkOrderNo());
            return brief(existing);
        }
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderNo(noGenerator.next());
        workOrder.setEventId(command.getEventId());
        workOrder.setEventNo(command.getEventNo());
        workOrder.setCategoryCode(command.getCategoryCode());
        workOrder.setPriority(command.getPriority());
        workOrder.setAssigneeId(command.getAssigneeId());
        workOrder.setAssigneeName(command.getAssigneeName());
        // 管理员派单对象即责任组长
        workOrder.setLeaderId(command.getAssigneeId());
        workOrder.setLeaderName(command.getAssigneeName());
        workOrder.setDeptCode(command.getDeptCode());
        workOrder.setStatus(WorkOrderStatus.PENDING_ACCEPT);
        workOrder.setCreatorId(command.getCreatorId());
        workOrder.setAssignTime(LocalDateTime.now());
        workOrder.setReturnCount(0);
        workOrder.setReassignCount(0);
        try {
            workOrderMapper.insert(workOrder);
        } catch (DuplicateKeyException e) {
            existing = findByEventId(command.getEventId());
            if (existing != null) {
                return brief(existing);
            }
            throw e;
        }
        saveLog(workOrder, "CREATE", null, WorkOrderStatus.PENDING_ACCEPT,
                command.getCreatorId(), RoleConstants.ADMIN, "审核通过，创建工单并派单", null);
        log.info("工单创建成功 workOrderNo={} eventNo={} assigneeId={}",
                workOrder.getWorkOrderNo(), workOrder.getEventNo(), workOrder.getAssigneeId());
        publisher.publishAssigned(workOrder, command.getCreatorId());
        if (environment.getProperty("demo.fault.create-workorder", Boolean.class, false)) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "故障注入：创建工单后模拟失败，用于验证 Seata 回滚");
        }
        return brief(workOrder);
    }

    public PageResult<WorkOrderListItemVO> page(WorkOrderQuery query, LoginUser user) {
        var wrapper = Wrappers.<WorkOrder>lambdaQuery();
        if (RoleConstants.WORKER.equals(user.getRole())) {
            wrapper.eq(WorkOrder::getAssigneeId, user.getUserId());
        } else if (!RoleConstants.ADMIN.equals(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(WorkOrder::getStatus, query.getStatus());
        }
        if (query.getAssigneeId() != null && RoleConstants.ADMIN.equals(user.getRole())) {
            wrapper.eq(WorkOrder::getAssigneeId, query.getAssigneeId());
        }
        if (StringUtils.hasText(query.getEventNo())) {
            wrapper.eq(WorkOrder::getEventNo, query.getEventNo());
        }
        if (StringUtils.hasText(query.getCategoryCode())) {
            wrapper.eq(WorkOrder::getCategoryCode, query.getCategoryCode());
        }
        // 优先级按紧急程度排序：URGENT > HIGH > MEDIUM > LOW；管理员默认待验收置顶
        if ("priority".equalsIgnoreCase(query.getSortBy())) {
            String direction = "asc".equalsIgnoreCase(query.getOrder()) ? "ASC" : "DESC";
            wrapper.last("ORDER BY FIELD(priority, 'LOW','MEDIUM','HIGH','URGENT') " + direction
                    + ", create_time DESC");
        } else if (RoleConstants.ADMIN.equals(user.getRole())) {
            wrapper.last("ORDER BY CASE WHEN status = 'PENDING_ACCEPTANCE' THEN 0 ELSE 1 END, create_time DESC");
        } else {
            wrapper.orderByDesc(WorkOrder::getCreateTime);
        }
        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(page.convert(this::toListItem));
    }

    public WorkOrderDetailVO detail(Long id, LoginUser user) {
        return buildDetail(requireVisible(id, user));
    }

    /**
     * 组长查看本组工单（含待分配、待复核）。
     */
    public PageResult<WorkOrderListItemVO> teamPage(WorkOrderQuery query, LoginUser user) {
        requireWorker(user);
        WorkerProfile self = fetchWorkerProfile(user.getUserId());
        if (!"LEADER".equals(self.getWorkerType())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅组长可查看组内工单");
        }
        var wrapper = Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getLeaderId, user.getUserId());
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(WorkOrder::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getEventNo())) {
            wrapper.eq(WorkOrder::getEventNo, query.getEventNo());
        }
        wrapper.orderByDesc(WorkOrder::getCreateTime);
        Page<WorkOrder> page = workOrderMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        return PageResult.of(page.convert(this::toListItem));
    }

    /**
     * 管理员删除工单：仅待接单可删，删除后事件退回待审核。
     */
    @org.apache.seata.spring.annotation.GlobalTransactional(name = "delete-workorder", rollbackFor = Exception.class)
    public void delete(Long id, LoginUser admin) {
        if (!RoleConstants.ADMIN.equals(admin.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可删除工单");
        }
        WorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!WorkOrderStatus.PENDING_ACCEPT.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "仅待接单的工单可删除，处理中的工单请使用改派");
        }
        workOrderMapper.deleteById(id);
        try {
            R<Void> response = eventClient.rollbackDispatch(workOrder.getEventId());
            if (response == null || !response.isSuccess()) {
                throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,
                        "事件回滚失败：" + (response == null ? "无响应" : response.getMessage()));
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "事件回滚失败：" + e.getMessage());
        }
        log.info("管理员删除工单 workOrderNo={} eventId={} adminId={}",
                workOrder.getWorkOrderNo(), workOrder.getEventId(), admin.getUserId());
    }

    /**
     * 按处理人统计在手工单数，供派单/分配时做工作量平衡。
     */
    public List<Map<String, Object>> loads(String ids) {
        List<Long> workerIds = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long workerId : workerIds) {
            Long active = workOrderMapper.selectCount(Wrappers.<WorkOrder>lambdaQuery()
                    .eq(WorkOrder::getAssigneeId, workerId)
                    .in(WorkOrder::getStatus, WorkOrderStatus.PENDING_ACCEPT, WorkOrderStatus.PROCESSING,
                            WorkOrderStatus.PENDING_LEADER_REVIEW));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workerId", workerId);
            row.put("activeCount", active == null ? 0 : active);
            result.add(row);
        }
        return result;
    }

    /**
     * 组内绩效：本月完成数、平均评分、平均处理时长、退回次数，并给出绩效分。
     */
    public List<Map<String, Object>> teamStats(LoginUser user) {
        requireWorker(user);
        WorkerProfile self = fetchWorkerProfile(user.getUserId());
        if (!"LEADER".equals(self.getWorkerType())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅组长可查看组内绩效");
        }
        List<Long> memberIds = new ArrayList<>();
        memberIds.add(user.getUserId());
        List<WorkOrder> orders = workOrderMapper.selectList(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getLeaderId, user.getUserId()));
        orders.stream().map(WorkOrder::getAssigneeId)
                .filter(id -> id != null && !memberIds.contains(id))
                .forEach(memberIds::add);
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        List<WorkRating> ratings = ratingMapper.selectList(Wrappers.<WorkRating>lambdaQuery()
                .in(WorkRating::getWorkerId, memberIds)
                .ge(WorkRating::getCreateTime, monthStart));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Long workerId : memberIds) {
            List<WorkOrder> workerOrders = orders.stream()
                    .filter(order -> workerId.equals(order.getAssigneeId())).toList();
            long completed = workerOrders.stream()
                    .filter(order -> WorkOrderStatus.COMPLETED.equals(order.getStatus()))
                    .filter(order -> order.getFinishTime() != null && !order.getFinishTime().isBefore(monthStart))
                    .count();
            long returns = workerOrders.stream().mapToLong(order ->
                    order.getReturnCount() == null ? 0 : order.getReturnCount()).sum();
            double avgRating = ratings.stream()
                    .filter(rating -> workerId.equals(rating.getWorkerId()))
                    .mapToInt(WorkRating::getRating).average().orElse(0);
            double avgHours = workerOrders.stream()
                    .filter(order -> order.getAcceptTime() != null && order.getSubmitTime() != null)
                    .mapToLong(order -> java.time.Duration.between(order.getAcceptTime(), order.getSubmitTime()).toMinutes())
                    .filter(minutes -> minutes >= 0)
                    .average().orElse(0) / 60.0;
            double score = completed * 4 + avgRating * 10 - returns * 2;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workerId", workerId);
            row.put("completedCount", completed);
            row.put("avgRating", Math.round(avgRating * 10) / 10.0);
            row.put("avgHours", Math.round(avgHours * 10) / 10.0);
            row.put("returnCount", returns);
            row.put("score", Math.max(0, Math.round(score * 10) / 10.0));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 居民在工单完成后评价处理人，每单只能评价一次。
     */
    @Transactional
    public void rate(Long id, RatingRequest request, LoginUser user) {
        WorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!WorkOrderStatus.COMPLETED.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "工单完成后才能评价");
        }
        EventSnapshot snapshot = loadEventSnapshot(workOrder.getEventId());
        if (snapshot == null || snapshot.getReporterId() == null
                || !snapshot.getReporterId().equals(user.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有上报人可以评价该工单");
        }
        WorkRating rating = new WorkRating();
        rating.setWorkOrderId(id);
        rating.setWorkOrderNo(workOrder.getWorkOrderNo());
        rating.setWorkerId(workOrder.getAssigneeId());
        rating.setReporterId(user.getUserId());
        rating.setRating(request.getRating());
        rating.setTags(request.getTags() == null ? null : String.join(",", request.getTags()));
        rating.setComment(request.getComment());
        try {
            ratingMapper.insert(rating);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.DUPLICATE_OPERATION, "该工单已评价，请勿重复提交");
        }
    }

    @Transactional
    public void accept(Long id, LoginUser user) {
        requireWorker(user);
        WorkOrder workOrder = requireVisible(id, user);
        if (!WorkOrderStatus.PENDING_ACCEPT.equals(workOrder.getStatus())) {
            if (WorkOrderStatus.PROCESSING.equals(workOrder.getStatus())) {
                throw new BizException(ErrorCode.WORK_ORDER_ALREADY_ACCEPTED);
            }
            throw new BizException(ErrorCode.STATE_CONFLICT, "当前工单状态不允许接单");
        }
        int rows = workOrderMapper.accept(id, user.getUserId());
        if (rows == 0) {
            throw new BizException(ErrorCode.WORK_ORDER_ALREADY_ACCEPTED);
        }
        saveLog(workOrder, "ACCEPT", WorkOrderStatus.PENDING_ACCEPT, WorkOrderStatus.PROCESSING,
                user.getUserId(), user.getRole(), "处理人员接单", null);
        publisher.publish(workOrder, WorkOrderStatus.PENDING_ACCEPT, WorkOrderStatus.PROCESSING,
                "PROCESSING", user.getUserId());
        log.info("工单接单 workOrderNo={} assigneeId={}", workOrder.getWorkOrderNo(), user.getUserId());
    }

    /**
     * 处理中可撤销接单：工单退回待接单，组长可重新分配或自己处理。
     */
    @Transactional
    public void cancelAccept(Long id, LoginUser user) {
        requireWorker(user);
        WorkOrder workOrder = requireVisible(id, user);
        if (!user.getUserId().equals(workOrder.getAssigneeId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有当前处理人可以撤销接单");
        }
        if (!WorkOrderStatus.PROCESSING.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "只有处理中的工单可以撤销接单");
        }
        int rows = workOrderMapper.cancelAccept(id, user.getUserId());
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "工单状态已变化，请刷新后重试");
        }
        saveLog(workOrder, "CANCEL_ACCEPT", WorkOrderStatus.PROCESSING, WorkOrderStatus.PENDING_ACCEPT,
                user.getUserId(), user.getRole(), "撤销接单，工单退回待接单", null);
        log.info("工单撤销接单 workOrderNo={} assigneeId={}", workOrder.getWorkOrderNo(), user.getUserId());
    }

    /**
     * 撤回已提交的处理结果：工单回到处理中，可重新整改后再提交。
     */
    @Transactional
    public void withdrawSubmit(Long id, LoginUser user) {
        requireWorker(user);
        WorkOrder workOrder = requireVisible(id, user);
        if (!user.getUserId().equals(workOrder.getAssigneeId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有当前处理人可以撤回处理结果");
        }
        String status = workOrder.getStatus();
        if (!WorkOrderStatus.PENDING_LEADER_REVIEW.equals(status)
                && !WorkOrderStatus.PENDING_ACCEPTANCE.equals(status)) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "当前状态没有可撤回的处理结果");
        }
        if (WorkOrderStatus.PENDING_ACCEPTANCE.equals(status)
                && workOrder.getLeaderId() != null
                && !workOrder.getLeaderId().equals(workOrder.getAssigneeId())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "组长已复核通过，无法撤回，请联系管理员退回");
        }
        int rows = workOrderMapper.withdrawSubmit(id, user.getUserId());
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "工单状态已变化，请刷新后重试");
        }
        // 清空上一次 AI 核实结论，避免撤回后仍展示过期结果
        workOrderMapper.update(null, Wrappers.<WorkOrder>lambdaUpdate()
                .eq(WorkOrder::getId, id)
                .set(WorkOrder::getAiVerifyResult, null)
                .set(WorkOrder::getAiVerifyTime, null));
        saveLog(workOrder, "WITHDRAW_SUBMIT", status, WorkOrderStatus.PROCESSING,
                user.getUserId(), user.getRole(), "撤回已提交的处理结果，继续整改", null);
        log.info("工单撤回提交 workOrderNo={} assigneeId={} from={}",
                workOrder.getWorkOrderNo(), user.getUserId(), status);
    }

    /**
     * 删除本人提交的过程记录（仅 PROCESS），便于撤销误传的照片与文字。
     */
    @Transactional
    public void deleteProcessLog(Long id, Long logId, LoginUser user) {
        requireWorker(user);
        WorkOrder workOrder = requireVisible(id, user);
        if (!user.getUserId().equals(workOrder.getAssigneeId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有当前处理人可以删除处理记录");
        }
        if (!WorkOrderStatus.PROCESSING.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "只有处理中的工单可以删除处理记录");
        }
        WorkOrderLog logRecord = workOrderLogMapper.selectById(logId);
        if (logRecord == null || !id.equals(logRecord.getWorkOrderId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "处理记录不存在");
        }
        if (!"PROCESS".equals(logRecord.getAction())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "只能删除过程记录");
        }
        workOrderLogMapper.deleteById(logId);
        log.info("删除过程记录 workOrderNo={} logId={} operator={}",
                workOrder.getWorkOrderNo(), logId, user.getUserId());
    }

    @Transactional
    public void addProcess(Long id, ProcessRequest request, LoginUser user) {
        requireWorker(user);
        WorkOrder workOrder = requireVisible(id, user);
        if (!WorkOrderStatus.PROCESSING.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "只有处理中的工单可以添加处理记录");
        }
        if (!StringUtils.hasText(request.getContent())
                && (request.getAttachmentIds() == null || request.getAttachmentIds().isEmpty())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "处理说明与照片至少填写一项");
        }
        saveLog(workOrder, "PROCESS", WorkOrderStatus.PROCESSING, WorkOrderStatus.PROCESSING,
                user.getUserId(), user.getRole(), request.getContent(), joinIds(request.getAttachmentIds()));
    }

    /**
     * 过程记录自动保存：同一处理人只维护一条 PROCESS 记录，内容或照片变化时原位更新；
     * 文本与照片都清空时删除该记录，避免留下空白条目。
     */
    @Transactional
    public void saveProcess(Long id, ProcessRequest request, LoginUser user) {
        requireWorker(user);
        WorkOrder workOrder = requireVisible(id, user);
        if (!user.getUserId().equals(workOrder.getAssigneeId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有当前处理人可以保存过程记录");
        }
        if (!WorkOrderStatus.PROCESSING.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "只有处理中的工单可以保存过程记录");
        }
        String content = StringUtils.hasText(request.getContent()) ? request.getContent().trim() : null;
        String attachmentIds = joinIds(request.getAttachmentIds());
        WorkOrderLog existing = workOrderLogMapper.selectOne(Wrappers.<WorkOrderLog>lambdaQuery()
                .eq(WorkOrderLog::getWorkOrderId, id)
                .eq(WorkOrderLog::getAction, "PROCESS")
                .eq(WorkOrderLog::getOperatorId, user.getUserId())
                .orderByDesc(WorkOrderLog::getId)
                .last("LIMIT 1"));
        if (content == null && attachmentIds == null) {
            if (existing != null) {
                workOrderLogMapper.deleteById(existing.getId());
                log.info("过程记录清空后删除 workOrderNo={} logId={}", workOrder.getWorkOrderNo(), existing.getId());
            }
            return;
        }
        if (existing == null) {
            saveLog(workOrder, "PROCESS", WorkOrderStatus.PROCESSING, WorkOrderStatus.PROCESSING,
                    user.getUserId(), user.getRole(), content, attachmentIds);
            return;
        }
        workOrderLogMapper.update(null, Wrappers.<WorkOrderLog>lambdaUpdate()
                .eq(WorkOrderLog::getId, existing.getId())
                .set(WorkOrderLog::getContent, content)
                .set(WorkOrderLog::getAttachmentIds, attachmentIds)
                .set(WorkOrderLog::getCreateTime, LocalDateTime.now()));
    }

    @Transactional
    public void submit(Long id, SubmitRequest request, LoginUser user) {
        requireWorker(user);
        WorkOrder workOrder = requireVisible(id, user);
        if (!WorkOrderStatus.PROCESSING.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "只有处理中的工单可以提交结果");
        }
        boolean leaderSelf = workOrder.getLeaderId() != null && workOrder.getLeaderId().equals(user.getUserId());
        String target = leaderSelf ? WorkOrderStatus.PENDING_ACCEPTANCE : WorkOrderStatus.PENDING_LEADER_REVIEW;
        bindResultAttachments(id, request.getAttachmentIds(), user.getUserId());
        int rows = workOrderMapper.submit(id, user.getUserId(), target);
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "工单状态已变化，请刷新后重试");
        }
        saveLog(workOrder, "SUBMIT", WorkOrderStatus.PROCESSING, target,
                user.getUserId(), user.getRole(), request.getContent(), joinIds(request.getAttachmentIds()));
        if (leaderSelf) {
            publisher.publish(workOrder, WorkOrderStatus.PROCESSING, WorkOrderStatus.PENDING_ACCEPTANCE,
                    "PENDING_ACCEPTANCE", user.getUserId());
        }
        log.info("工单提交结果 workOrderNo={} assigneeId={} target={}",
                workOrder.getWorkOrderNo(), user.getUserId(), target);
        registerAiVerifyAfterCommit(id);
    }

    /**
     * 组长复核组员提交的处理结果：通过转管理员验收，退回则回到处理中并计入退回次数。
     */
    @Transactional
    public void leaderReview(Long id, LeaderReviewRequest request, LoginUser user) {
        requireWorker(user);
        WorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (workOrder.getLeaderId() == null || !workOrder.getLeaderId().equals(user.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有责任组长可以复核");
        }
        if (!WorkOrderStatus.PENDING_LEADER_REVIEW.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "当前工单不在待组长复核状态");
        }
        boolean pass = "PASS".equals(request.getResult());
        if (!pass && !StringUtils.hasText(request.getRemark())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "退回时必须填写复核意见");
        }
        int rows = pass
                ? workOrderMapper.leaderPass(id, user.getUserId(), request.getRemark())
                : workOrderMapper.leaderReject(id, user.getUserId(), request.getRemark());
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "工单状态已变化，请刷新后重试");
        }
        saveLog(workOrder, pass ? "LEADER_PASS" : "LEADER_REJECT",
                WorkOrderStatus.PENDING_LEADER_REVIEW,
                pass ? WorkOrderStatus.PENDING_ACCEPTANCE : WorkOrderStatus.PROCESSING,
                user.getUserId(), user.getRole(), request.getRemark(), null);
        if (pass) {
            publisher.publish(workOrder, WorkOrderStatus.PENDING_LEADER_REVIEW, WorkOrderStatus.PENDING_ACCEPTANCE,
                    "PENDING_ACCEPTANCE", user.getUserId());
        }
        log.info("组长复核 workOrderNo={} leaderId={} result={}",
                workOrder.getWorkOrderNo(), user.getUserId(), request.getResult());
    }

    /**
     * 组长把工单分配给本组组员。
     */
    @Transactional
    public void assign(Long id, AssignRequest request, LoginUser user) {
        requireWorker(user);
        WorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (workOrder.getLeaderId() == null || !workOrder.getLeaderId().equals(user.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只有责任组长可以分配工单");
        }
        if (!WorkOrderStatus.PENDING_ACCEPT.equals(workOrder.getStatus())
                && !WorkOrderStatus.PROCESSING.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "当前状态不允许分配");
        }
        WorkerProfile member = fetchWorker(request.getAssigneeId());
        if (member.getLeaderId() == null || !member.getLeaderId().equals(user.getUserId())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "只能分配给自己组内的组员");
        }
        String fromStatus = workOrder.getStatus();
        int rows = workOrderMapper.assign(id, user.getUserId(), member.getUserId(),
                member.getNickname(), member.getDeptCode());
        if (rows == 0) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "工单状态已变化，请刷新后重试");
        }
        saveLog(workOrder, "ASSIGN", fromStatus, WorkOrderStatus.PENDING_ACCEPT,
                user.getUserId(), user.getRole(), request.getRemark(), null);
        log.info("组长分配工单 workOrderNo={} leaderId={} assigneeId={}",
                workOrder.getWorkOrderNo(), user.getUserId(), member.getUserId());
    }

    /**
     * 事务提交后再异步触发视觉核实，避免异步线程读不到未提交的整改记录。
     */
    private void registerAiVerifyAfterCommit(Long workOrderId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    aiVerifyService.verifyAsync(workOrderId);
                }
            });
        } else {
            aiVerifyService.verifyAsync(workOrderId);
        }
    }

    @Transactional
    public void acceptance(Long id, AcceptanceRequest request, LoginUser admin) {
        if (!RoleConstants.ADMIN.equals(admin.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        WorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!WorkOrderStatus.PENDING_ACCEPTANCE.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "当前工单不在待验收状态");
        }
        boolean pass = "PASS".equals(request.getResult());
        if (!pass && !StringUtils.hasText(request.getRemark())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "验收不通过时必须填写意见");
        }
        int rows = pass
                ? workOrderMapper.acceptPass(id, request.getRemark())
                : workOrderMapper.acceptReject(id, request.getRemark());
        if (rows == 0) {
            throw new BizException(ErrorCode.DUPLICATE_OPERATION, "工单已被处理，请刷新后重试");
        }

        WorkOrderAcceptance acceptance = new WorkOrderAcceptance();
        acceptance.setWorkOrderId(id);
        acceptance.setWorkOrderNo(workOrder.getWorkOrderNo());
        acceptance.setAcceptorId(admin.getUserId());
        acceptance.setResult(pass ? "PASS" : "REJECT");
        acceptance.setRemark(request.getRemark());
        acceptanceMapper.insert(acceptance);

        saveLog(workOrder, pass ? "ACCEPT_PASS" : "ACCEPT_REJECT",
                WorkOrderStatus.PENDING_ACCEPTANCE,
                pass ? WorkOrderStatus.COMPLETED : WorkOrderStatus.PROCESSING,
                admin.getUserId(), admin.getRole(), request.getRemark(), null);

        if (pass) {
            publisher.publish(workOrder, WorkOrderStatus.PENDING_ACCEPTANCE, WorkOrderStatus.COMPLETED,
                    "CLOSED", admin.getUserId());
        } else {
            publisher.publish(workOrder, WorkOrderStatus.PENDING_ACCEPTANCE, WorkOrderStatus.PROCESSING,
                    "PROCESSING", admin.getUserId());
        }
        log.info("工单验收 workOrderNo={} result={} acceptorId={}",
                workOrder.getWorkOrderNo(), request.getResult(), admin.getUserId());
    }

    public void reassign(Long id, ReassignRequest request, LoginUser admin) {
        if (!RoleConstants.ADMIN.equals(admin.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        WorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!WorkOrderStatus.PENDING_ACCEPT.equals(workOrder.getStatus())
                && !WorkOrderStatus.PROCESSING.equals(workOrder.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "当前工单状态不允许改派");
        }
        WorkerProfile worker = fetchWorker(request.getAssigneeId());

        transactionTemplate.executeWithoutResult(status -> {
            int rows = workOrderMapper.reassign(id, worker.getUserId(), worker.getNickname(), worker.getDeptCode());
            if (rows == 0) {
                throw new BizException(ErrorCode.STATE_CONFLICT, "工单状态已变化，请刷新后重试");
            }
            WorkOrderReassign reassign = new WorkOrderReassign();
            reassign.setWorkOrderId(id);
            reassign.setFromAssigneeId(workOrder.getAssigneeId());
            reassign.setToAssigneeId(worker.getUserId());
            reassign.setReason(request.getReason());
            reassign.setOperatorId(admin.getUserId());
            reassignMapper.insert(reassign);
            saveLog(workOrder, "REASSIGN", workOrder.getStatus(), WorkOrderStatus.PENDING_ACCEPT,
                    admin.getUserId(), admin.getRole(), request.getReason(), null);
        });
        log.info("工单改派 workOrderNo={} from={} to={}", workOrder.getWorkOrderNo(),
                workOrder.getAssigneeId(), worker.getUserId());
    }

    public WorkOrderBrief findByEvent(Long eventId) {
        WorkOrder workOrder = findByEventId(eventId);
        return workOrder == null ? null : brief(workOrder);
    }

    /**
     * 批量查询事件对应的工单与居民评价，供事件列表展示星级与定位责任组长。
     */
    public List<com.city.platform.common.api.dto.EventRating> ratings(String eventIds) {
        if (!StringUtils.hasText(eventIds)) {
            return List.of();
        }
        List<Long> ids = Arrays.stream(eventIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<WorkOrder> orders = workOrderMapper.selectList(Wrappers.<WorkOrder>lambdaQuery()
                .in(WorkOrder::getEventId, ids));
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(WorkOrder::getId).toList();
        Map<Long, WorkRating> rated = new LinkedHashMap<>();
        ratingMapper.selectList(Wrappers.<WorkRating>lambdaQuery().in(WorkRating::getWorkOrderId, orderIds))
                .forEach(item -> rated.put(item.getWorkOrderId(), item));
        return orders.stream().map(order -> {
            com.city.platform.common.api.dto.EventRating vo = new com.city.platform.common.api.dto.EventRating();
            vo.setEventId(order.getEventId());
            vo.setWorkOrderId(order.getId());
            vo.setWorkOrderNo(order.getWorkOrderNo());
            vo.setWorkOrderStatus(order.getStatus());
            vo.setLeaderId(order.getLeaderId());
            vo.setLeaderName(order.getLeaderName());
            vo.setAssigneeId(order.getAssigneeId());
            vo.setAssigneeName(order.getAssigneeName());
            WorkRating rating = rated.get(order.getId());
            if (rating != null) {
                vo.setRating(rating.getRating());
                vo.setTags(rating.getTags());
                vo.setComment(rating.getComment());
            }
            return vo;
        }).toList();
    }

    private WorkOrder findByEventId(Long eventId) {
        return workOrderMapper.selectOne(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getEventId, eventId).last("LIMIT 1"));
    }

    private WorkOrder requireVisible(Long id, LoginUser user) {
        WorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        // 组员只能看自己的工单，责任组长可以看本组全部工单
        if (RoleConstants.WORKER.equals(user.getRole())
                && !workOrder.getAssigneeId().equals(user.getUserId())
                && (workOrder.getLeaderId() == null || !workOrder.getLeaderId().equals(user.getUserId()))) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_OWNED);
        }
        if (!RoleConstants.WORKER.equals(user.getRole()) && !RoleConstants.ADMIN.equals(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return workOrder;
    }

    private void requireWorker(LoginUser user) {
        if (!RoleConstants.WORKER.equals(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅处理人员可执行该操作");
        }
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

    private WorkerProfile fetchWorkerProfile(Long workerId) {
        return fetchWorker(workerId);
    }

    private EventSnapshot loadEventSnapshot(Long eventId) {
        try {
            R<EventSnapshot> response = eventClient.snapshot(eventId);
            return response != null && response.isSuccess() ? response.getData() : null;
        } catch (Exception e) {
            log.warn("查询事件快照失败 eventId={}: {}", eventId, e.getMessage());
            return null;
        }
    }

    private void bindResultAttachments(Long workOrderId, List<Long> attachmentIds, Long ownerId) {
        try {
            R<Void> response = eventClient.bindAttachments(
                    new com.city.platform.common.api.dto.BindAttachmentCommand(
                            attachmentIds, workOrderId, ownerId, "WORKORDER_RESULT"));
            if (response == null || !response.isSuccess()) {
                throw new BizException(ErrorCode.ATTACHMENT_INVALID, "现场照片绑定失败");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "附件服务不可用：" + e.getMessage());
        }
    }

    private void saveLog(WorkOrder workOrder, String action, String fromStatus, String toStatus,
                         Long operatorId, String role, String content, String attachmentIds) {
        WorkOrderLog logRecord = new WorkOrderLog();
        logRecord.setWorkOrderId(workOrder.getId());
        logRecord.setWorkOrderNo(workOrder.getWorkOrderNo());
        logRecord.setAction(action);
        logRecord.setFromStatus(fromStatus);
        logRecord.setToStatus(toStatus);
        logRecord.setOperatorId(operatorId);
        logRecord.setOperatorRole(role);
        logRecord.setContent(content);
        logRecord.setAttachmentIds(attachmentIds);
        workOrderLogMapper.insert(logRecord);
    }

    private WorkOrderDetailVO buildDetail(WorkOrder workOrder) {
        WorkOrderDetailVO vo = new WorkOrderDetailVO();
        BeanUtils.copyProperties(workOrder, vo);
        List<WorkOrderLogVO> logs = workOrderLogMapper.selectList(Wrappers.<WorkOrderLog>lambdaQuery()
                        .eq(WorkOrderLog::getWorkOrderId, workOrder.getId())
                        .orderByAsc(WorkOrderLog::getId))
                .stream().map(item -> {
                    WorkOrderLogVO logVO = new WorkOrderLogVO();
                    BeanUtils.copyProperties(item, logVO);
                    return logVO;
                }).toList();
        vo.setLogs(logs);

        WorkOrderAcceptance acceptance = acceptanceMapper.selectOne(Wrappers.<WorkOrderAcceptance>lambdaQuery()
                .eq(WorkOrderAcceptance::getWorkOrderId, workOrder.getId())
                .orderByDesc(WorkOrderAcceptance::getId).last("LIMIT 1"));
        if (acceptance != null) {
            WorkOrderAcceptanceVO acceptanceVO = new WorkOrderAcceptanceVO();
            BeanUtils.copyProperties(acceptance, acceptanceVO);
            vo.setAcceptance(acceptanceVO);
        }

        vo.setAiVerify(aiVerifyService.parse(workOrder.getAiVerifyResult()));

        WorkRating rating = ratingMapper.selectOne(Wrappers.<WorkRating>lambdaQuery()
                .eq(WorkRating::getWorkOrderId, workOrder.getId()).last("LIMIT 1"));
        if (rating != null) {
            WorkRatingVO ratingVO = new WorkRatingVO();
            BeanUtils.copyProperties(rating, ratingVO);
            vo.setRating(ratingVO);
        }

        try {
            R<EventSnapshot> response = eventClient.snapshot(workOrder.getEventId());
            if (response != null && response.isSuccess()) {
                vo.setEvent(response.getData());
            }
        } catch (Exception e) {
            log.warn("查询事件快照失败（不影响工单详情） eventId={}: {}",
                    workOrder.getEventId(), e.getMessage());
        }
        try {
            R<List<RectifyImageRef>> photos = eventClient.eventAttachments(workOrder.getEventId());
            if (photos != null && photos.isSuccess()) {
                vo.setEventPhotos(photos.getData());
            }
        } catch (Exception e) {
            log.warn("查询事件原图失败（不影响工单详情） eventId={}: {}",
                    workOrder.getEventId(), e.getMessage());
        }
        return vo;
    }

    private WorkOrderListItemVO toListItem(WorkOrder workOrder) {
        WorkOrderListItemVO vo = new WorkOrderListItemVO();
        BeanUtils.copyProperties(workOrder, vo);
        return vo;
    }

    private WorkOrderBrief brief(WorkOrder workOrder) {
        return new WorkOrderBrief(workOrder.getId(), workOrder.getWorkOrderNo(),
                workOrder.getStatus(), workOrder.getAssigneeId());
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return String.join(",", ids.stream().map(String::valueOf).toList());
    }
}
