package com.city.platform.event.service;

import com.city.platform.common.api.dto.CreateWorkOrderCommand;
import com.city.platform.common.api.dto.WorkOrderBrief;
import com.city.platform.common.api.dto.WorkerProfile;
import com.city.platform.common.api.feign.WorkOrderClient;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.R;
import com.city.platform.event.constant.EventStatus;
import com.city.platform.event.entity.Event;
import com.city.platform.event.entity.EventAuditLog;
import com.city.platform.event.entity.EventStatusLog;
import com.city.platform.event.mapper.EventAuditLogMapper;
import com.city.platform.event.mapper.EventMapper;
import com.city.platform.event.mapper.EventStatusLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 事件派单执行：审核通过派单与自动分流派单共用同一套跨库事务逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventDispatchService {

    private final EventMapper eventMapper;
    private final EventStatusLogMapper statusLogMapper;
    private final EventAuditLogMapper auditLogMapper;
    private final WorkOrderClient workOrderClient;
    private final EventCacheService eventCacheService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 将待审核事件派给组长并创建工单。
     *
     * @param operatorId 操作人；自动派单传 0 表示系统
     * @param decision   APPROVE 人工审核 / AUTO_APPROVE 系统自动派单
     * @param remark     审核备注
     */
    @GlobalTransactional(name = "dispatch-and-create-workorder", rollbackFor = Exception.class)
    public WorkOrderBrief dispatch(Long eventId, WorkerProfile leader, String categoryCode, String priority,
                                   String areaCode, Long operatorId, String operatorRole,
                                   String decision, String action, String remark) {
        WorkOrderBrief result = transactionTemplate.execute(status -> {
            Event event = eventMapper.selectById(eventId);
            if (event == null) {
                throw new BizException(ErrorCode.EVENT_NOT_FOUND);
            }
            EventStateMachine.assertTransit(event.getStatus(), EventStatus.DISPATCHED);

            int rows = "AUTO_APPROVE".equals(decision)
                    ? eventMapper.autoDispatch(eventId, categoryCode, priority, areaCode)
                    : eventMapper.approveWithFields(eventId, categoryCode, priority, areaCode, operatorId);
            if (rows == 0) {
                throw new BizException(ErrorCode.DUPLICATE_OPERATION, "事件已被处理，请刷新后重试");
            }

            EventAuditLog auditLog = new EventAuditLog();
            auditLog.setEventId(eventId);
            auditLog.setAuditorId(operatorId);
            auditLog.setDecision(decision);
            auditLog.setBeforeCategory(event.getCategoryCode());
            auditLog.setAfterCategory(categoryCode);
            auditLog.setBeforePriority(event.getPriority());
            auditLog.setAfterPriority(priority);
            auditLog.setReason(remark);
            auditLogMapper.insert(auditLog);

            EventStatusLog statusLog = new EventStatusLog();
            statusLog.setEventId(eventId);
            statusLog.setEventNo(event.getEventNo());
            statusLog.setFromStatus(EventStatus.PENDING_REVIEW);
            statusLog.setToStatus(EventStatus.DISPATCHED);
            statusLog.setAction(action);
            statusLog.setOperatorId(operatorId);
            statusLog.setOperatorRole(operatorRole);
            statusLog.setRemark(remark);
            statusLogMapper.insert(statusLog);

            CreateWorkOrderCommand command = new CreateWorkOrderCommand(
                    eventId, event.getEventNo(), categoryCode, priority,
                    leader.getUserId(), leader.getNickname(), leader.getDeptCode(), operatorId);
            R<WorkOrderBrief> response;
            try {
                response = workOrderClient.create(command);
            } catch (Exception e) {
                log.error("创建工单失败，事件状态将回滚 eventId={}", eventId, e);
                throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "创建工单失败：" + e.getMessage());
            }
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BizException(ErrorCode.SERVICE_UNAVAILABLE,
                        "创建工单失败：" + (response == null ? "无响应" : response.getMessage()));
            }
            log.info("事件派单完成 eventId={} eventNo={} workOrderNo={} leaderId={} operator={} decision={}",
                    eventId, event.getEventNo(), response.getData().getWorkOrderNo(),
                    leader.getUserId(), operatorId, decision);
            return response.getData();
        });
        eventCacheService.evictAfterChange(eventId);
        return result;
    }
}
