package com.city.platform.event.service;

import com.city.platform.common.api.dto.WorkerProfile;
import com.city.platform.common.api.feign.UserClient;
import com.city.platform.common.core.model.R;
import com.city.platform.event.constant.EventStatus;
import com.city.platform.event.entity.Event;
import com.city.platform.event.mapper.EventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 智能分流：中/高/紧急优先级事件在片区有在岗组长时自动派单，低优先级保留人工审核。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoDispatchService {

    private static final List<String> AUTO_LEVELS = List.of("URGENT", "HIGH", "MEDIUM");

    private final EventMapper eventMapper;
    private final UserClient userClient;
    private final EventDispatchService dispatchService;

    /**
     * 上报或 AI 复评后触发；任何异常都不影响事件本身的流转，失败即回落到人工审核。
     */
    public void tryDispatch(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null || !EventStatus.PENDING_REVIEW.equals(event.getStatus())) {
            return;
        }
        String priority = StringUtils.hasText(event.getAiReviewPriority())
                ? event.getAiReviewPriority() : event.getPriority();
        if (!AUTO_LEVELS.contains(priority)) {
            return;
        }
        String areaCode = event.getAreaCode();
        if (!StringUtils.hasText(areaCode)) {
            return;
        }
        List<WorkerProfile> leaders;
        try {
            R<List<WorkerProfile>> response = userClient.leaders(areaCode);
            if (response == null || !response.isSuccess() || response.getData() == null
                    || response.getData().isEmpty()) {
                log.info("自动派单跳过：片区无在岗组长 eventId={} areaCode={}", eventId, areaCode);
                return;
            }
            leaders = response.getData();
        } catch (Exception e) {
            log.warn("自动派单查询组长失败，转为人工审核 eventId={}: {}", eventId, e.getMessage());
            return;
        }
        WorkerProfile leader = leaders.get(0);
        try {
            dispatchService.dispatch(eventId, leader, event.getCategoryCode(), priority, areaCode,
                    0L, "SYSTEM", "AUTO_APPROVE", "AUTO_DISPATCH",
                    "按片区自动派单：匹配组长" + leader.getNickname());
        } catch (Exception e) {
            log.warn("自动派单失败，保留人工审核 eventId={}: {}", eventId, e.getMessage());
        }
    }
}
