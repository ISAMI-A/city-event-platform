package com.city.platform.event.service;

import com.city.platform.common.api.dto.WorkOrderStatusMessage;
import com.city.platform.event.entity.Event;
import com.city.platform.event.entity.EventStatusLog;
import com.city.platform.event.mapper.EventMapper;
import com.city.platform.event.mapper.EventStatusLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工单状态消息消费：按当前事件状态做条件更新，保证幂等与乱序安全。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventStatusSyncService {

    private final EventMapper eventMapper;
    private final EventStatusLogMapper statusLogMapper;
    private final EventCacheService eventCacheService;

    @Transactional
    public void sync(WorkOrderStatusMessage message) {
        Event event = eventMapper.selectById(message.getEventId());
        if (event == null) {
            log.warn("事件不存在，忽略消息 eventId={} messageId={}", message.getEventId(), message.getMessageId());
            return;
        }
        String target = message.getEventTargetStatus();
        if (!EventStateMachine.canTransit(event.getStatus(), target)) {
            log.warn("事件状态迁移不合法，忽略消息 eventId={} current={} target={} messageId={}",
                    event.getId(), event.getStatus(), target, message.getMessageId());
            return;
        }
        int rows = eventMapper.updateStatus(event.getId(), event.getStatus(), target);
        if (rows == 0) {
            log.warn("事件状态已被并发修改，忽略消息 eventId={} messageId={}", event.getId(), message.getMessageId());
            return;
        }
        EventStatusLog statusLog = new EventStatusLog();
        statusLog.setEventId(event.getId());
        statusLog.setEventNo(event.getEventNo());
        statusLog.setFromStatus(event.getStatus());
        statusLog.setToStatus(target);
        statusLog.setAction("MQ_SYNC");
        statusLog.setOperatorId(0L);
        statusLog.setOperatorRole("SYSTEM");
        statusLog.setRemark("工单状态同步：" + message.getWorkOrderNo());
        statusLogMapper.insert(statusLog);

        log.info("事件状态同步成功 eventId={} from={} to={} workOrderNo={} messageId={}",
                event.getId(), event.getStatus(), target, message.getWorkOrderNo(), message.getMessageId());
        eventCacheService.evictAfterChange(event.getId());
    }
}
