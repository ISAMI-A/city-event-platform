package com.city.platform.event.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.platform.common.api.dto.EventRating;
import com.city.platform.common.api.dto.WorkOrderAssignedMessage;
import com.city.platform.common.api.dto.WorkOrderBrief;
import com.city.platform.common.api.dto.WorkerProfile;
import com.city.platform.common.api.feign.UserClient;
import com.city.platform.common.api.feign.WorkOrderClient;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.LoginUser;
import com.city.platform.event.dto.ForwardNoticeRequest;
import com.city.platform.event.dto.RemindRequest;
import com.city.platform.event.dto.TeamNoticeRequest;
import com.city.platform.event.entity.Event;
import com.city.platform.event.entity.Notification;
import com.city.platform.event.mapper.EventMapper;
import com.city.platform.event.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工单分配通知消费与站内通知中心。
 * 消息发送成功不代表业务成功，必须以工单库的当前状态为准。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final WorkOrderClient workOrderClient;
    private final UserClient userClient;
    private final EventMapper eventMapper;

    @Transactional
    public void handleAssigned(WorkOrderAssignedMessage message) {
        R<WorkOrderBrief> response;
        try {
            response = workOrderClient.getByEvent(message.getEventId());
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "工单校验失败：" + e.getMessage());
        }
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "工单不存在或服务不可用");
        }
        WorkOrderBrief workOrder = response.getData();
        boolean matches = message.getWorkOrderId().equals(workOrder.getWorkOrderId())
                && "PENDING_ACCEPT".equals(workOrder.getStatus())
                && message.getAssigneeId().equals(workOrder.getAssigneeId());
        if (!matches) {
            log.warn("工单状态与消息不一致，跳过通知 workOrderNo={} messageStatus={} actualStatus={}",
                    message.getWorkOrderNo(), "PENDING_ACCEPT", workOrder.getStatus());
            return;
        }
        int rows = notificationMapper.insertIgnore(message.getAssigneeId(), "新工单待接单",
                "工单 " + message.getWorkOrderNo() + "（事件 " + message.getEventNo() + "）已分配给您，请及时接单处理。",
                "WORK_ORDER_ASSIGNED", message.getWorkOrderId());
        log.info("工单分配通知处理完成 workOrderNo={} assigneeId={} inserted={} messageId={}",
                message.getWorkOrderNo(), message.getAssigneeId(), rows, message.getMessageId());
    }

    public List<Notification> myNotifications(Long userId) {
        return notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getId)
                .last("LIMIT 50"));
    }

    /**
     * 单条通知详情：只能读取自己的通知。
     */
    public Notification detail(Long id, Long userId) {
        Notification item = notificationMapper.selectOne(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId));
        if (item == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        return item;
    }

    public long unreadCount(Long userId) {
        Long count = notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return count == null ? 0 : count;
    }

    public void markRead(Long id, Long userId) {
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId));
    }

    /**
     * 管理员提醒责任组长：带入居民评分与原话，要求组长跟进整改。
     */
    public void remind(RemindRequest request, LoginUser admin) {
        if (!RoleConstants.ADMIN.equals(admin.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可提醒组长");
        }
        Event event = eventMapper.selectById(request.getEventId());
        if (event == null) {
            throw new BizException(ErrorCode.EVENT_NOT_FOUND);
        }
        EventRating info = findRating(event.getId());
        if (info == null || info.getWorkOrderId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "事件尚未派单，无法提醒组长");
        }
        if (info.getLeaderId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "该工单暂未记录责任组长");
        }
        String starText = info.getRating() == null ? "暂未收到评分" : info.getRating() + " 星";
        StringBuilder content = new StringBuilder("事件《").append(event.getTitle())
                .append("》（").append(event.getEventNo()).append("）居民评价：").append(starText);
        if (StringUtils.hasText(info.getComment())) {
            content.append("，评价内容：").append(info.getComment());
        }
        if (StringUtils.hasText(request.getNote())) {
            content.append("。管理员补充：").append(request.getNote());
        }
        content.append("。请组长跟进复核并反馈整改情况。");
        notificationMapper.upsert(info.getLeaderId(), admin.getUserId(),
                "管理员提醒：请跟进居民反馈", content.toString(), "RATING_REMIND", info.getWorkOrderId());
        log.info("管理员提醒组长 eventId={} workOrderId={} leaderId={} adminId={}",
                event.getId(), info.getWorkOrderId(), info.getLeaderId(), admin.getUserId());
    }

    /**
     * 组长把居民反馈转达给具体组员，只能转达本组在岗组员。
     */
    public void forward(ForwardNoticeRequest request, LoginUser leader) {
        WorkerProfile self = requireLeader(leader);
        List<WorkerProfile> members = membersOf(leader.getUserId());
        WorkerProfile target = members.stream()
                .filter(item -> item.getUserId().equals(request.getMemberId()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.FORBIDDEN, "只能通知本组在岗组员"));
        StringBuilder content = new StringBuilder("组长").append(self.getNickname())
                .append("转达：工单 ").append(request.getWorkOrderId())
                .append(" 收到居民反馈，请尽快核实处理");
        if (StringUtils.hasText(request.getNote())) {
            content.append("。要求：").append(request.getNote());
        }
        content.append("。");
        notificationMapper.upsert(target.getUserId(), leader.getUserId(),
                "组长通知：居民反馈待跟进", content.toString(), "LEADER_FORWARD", request.getWorkOrderId());
        log.info("组长转达组员 leaderId={} memberId={} workOrderId={}",
                leader.getUserId(), target.getUserId(), request.getWorkOrderId());
    }

    /**
     * 组长向本组全部在岗组员发布注意事项，并给组长自己留一条发送回执。
     */
    public int teamNotice(TeamNoticeRequest request, LoginUser leader) {
        WorkerProfile self = requireLeader(leader);
        List<WorkerProfile> members = membersOf(leader.getUserId());
        if (members.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "本组暂无在岗组员");
        }
        long bizId = System.currentTimeMillis();
        for (WorkerProfile member : members) {
            notificationMapper.upsert(member.getUserId(), leader.getUserId(),
                    "组长注意事项（" + self.getNickname() + "）", request.getContent(), "TEAM_NOTICE", bizId);
        }
        String names = members.stream().limit(5).map(WorkerProfile::getNickname)
                .collect(java.util.stream.Collectors.joining("、"));
        String receivers = members.size() > 5
                ? names + " 等 " + members.size() + " 名组员" : names;
        String receipt = request.getContent() + "\n\n已发送给：" + receivers;
        notificationMapper.upsert(leader.getUserId(), leader.getUserId(),
                "已发布注意事项（本组 " + members.size() + " 人）", receipt, "TEAM_NOTICE_SENT", bizId);
        log.info("组长发布注意事项 leaderId={} members={} bizId={}", leader.getUserId(), members.size(), bizId);
        return members.size();
    }

    private EventRating findRating(Long eventId) {
        try {
            R<List<EventRating>> response = workOrderClient.ratings(String.valueOf(eventId));
            if (response == null || !response.isSuccess() || response.getData() == null
                    || response.getData().isEmpty()) {
                return null;
            }
            return response.getData().get(0);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "工单服务不可用：" + e.getMessage());
        }
    }

    private WorkerProfile requireLeader(LoginUser user) {
        if (!RoleConstants.WORKER.equals(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅处理人员可操作");
        }
        WorkerProfile profile = fetchProfile(user.getUserId());
        if (!"LEADER".equals(profile.getWorkerType())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅组长可操作");
        }
        return profile;
    }

    private List<WorkerProfile> membersOf(Long leaderId) {
        try {
            R<List<WorkerProfile>> response = userClient.members(leaderId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                return List.of();
            }
            return response.getData();
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "用户服务不可用：" + e.getMessage());
        }
    }

    private WorkerProfile fetchProfile(Long userId) {
        try {
            R<WorkerProfile> response = userClient.workerProfile(userId);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BizException(ErrorCode.FORBIDDEN,
                        response == null ? "处理人员校验失败" : response.getMessage());
            }
            return response.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "用户服务不可用：" + e.getMessage());
        }
    }
}
