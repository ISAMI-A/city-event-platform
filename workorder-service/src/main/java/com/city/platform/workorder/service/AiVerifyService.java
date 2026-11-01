package com.city.platform.workorder.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.platform.common.api.dto.DraftGenerateResult;
import com.city.platform.common.api.dto.EventSnapshot;
import com.city.platform.common.api.dto.RectifyImageRef;
import com.city.platform.common.api.dto.RectifyVerifyRequest;
import com.city.platform.common.api.dto.RectifyVerifyResult;
import com.city.platform.common.api.feign.AiClient;
import com.city.platform.common.api.feign.EventClient;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.context.LoginUser;
import com.city.platform.workorder.dto.AnalyzeDraftRequest;
import com.city.platform.workorder.dto.DraftRequest;
import com.city.platform.workorder.entity.WorkOrder;
import com.city.platform.workorder.entity.WorkOrderLog;
import com.city.platform.workorder.mapper.WorkOrderLogMapper;
import com.city.platform.workorder.mapper.WorkOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 工单整改核实：提交结果后异步分析，管理员验收时也可手动重新核实。
 * AI 结论仅用于辅助验收，不改变工单状态、不自动通过验收。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiVerifyService {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderLogMapper workOrderLogMapper;
    private final EventClient eventClient;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public RectifyVerifyResult verify(Long workOrderId) {
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        WorkOrderLog submitLog = latestSubmitLog(workOrderId);
        if (submitLog == null) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "工单尚未提交整改结果，无法核实");
        }

        List<RectifyImageRef> images = loadAttachments(submitLog.getAttachmentIds());
        RectifyVerifyResult result = requestAi(workOrder, submitLog.getContent(), images);
        save(workOrderId, result);
        log.info("工单 AI 整改核实 workOrderNo={} conclusion={} degraded={} images={}",
                workOrder.getWorkOrderNo(), result.getConclusion(), result.isDegraded(), images.size());
        return result;
    }

    /**
     * 调用 AI 服务做视觉核实，异常统一转换为业务异常。
     * 请求同时携带居民上报的原始描述与原图，供模型做整改前后对照。
     */
    private RectifyVerifyResult requestAi(WorkOrder workOrder, String content, List<RectifyImageRef> images) {
        EventSnapshot snapshot = loadSnapshot(workOrder.getEventId());
        List<RectifyImageRef> originalImages = loadEventImages(workOrder.getEventId());
        RectifyVerifyRequest request = new RectifyVerifyRequest(
                workOrder.getId(), workOrder.getWorkOrderNo(), workOrder.getEventNo(), workOrder.getCategoryCode(),
                snapshot == null ? null : snapshot.getTitle(),
                snapshot == null ? null : snapshot.getAddress(),
                snapshot == null ? null : snapshot.getDescription(),
                content, originalImages, images);
        try {
            R<RectifyVerifyResult> response = aiClient.verifyRectify(request);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BizException(ErrorCode.AI_UNAVAILABLE,
                        response == null ? "AI 服务无响应" : response.getMessage());
            }
            return response.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "AI 核实服务不可用：" + e.getMessage());
        }
    }

    private EventSnapshot loadSnapshot(Long eventId) {
        if (eventId == null) {
            return null;
        }
        try {
            R<EventSnapshot> response = eventClient.snapshot(eventId);
            return response != null && response.isSuccess() ? response.getData() : null;
        } catch (Exception e) {
            log.warn("查询事件快照失败 eventId={}: {}", eventId, e.getMessage());
            return null;
        }
    }

    private List<RectifyImageRef> loadEventImages(Long eventId) {
        if (eventId == null) {
            return List.of();
        }
        try {
            R<List<RectifyImageRef>> response = eventClient.eventAttachments(eventId);
            return response != null && response.isSuccess() && response.getData() != null
                    ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("查询事件原图失败 eventId={}: {}", eventId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 工人提交前预检：用当前编辑中的处理说明与已上传照片请求视觉模型，只返回建议，不落库、不改状态。
     */
    public RectifyVerifyResult analyzeDraft(Long workOrderId, AnalyzeDraftRequest request, LoginUser user) {
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (workOrder.getAssigneeId() == null || !workOrder.getAssigneeId().equals(user.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        String content = request.getContent();
        List<Long> ids = request.getAttachmentIds();
        if (!StringUtils.hasText(content) || ids == null || ids.isEmpty()) {
            WorkOrderLog submitLog = latestSubmitLog(workOrderId);
            if (submitLog == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "请先填写处理说明并上传现场照片，再让 AI 分析");
            }
            content = submitLog.getContent();
            ids = parseIds(submitLog.getAttachmentIds());
        }
        List<RectifyImageRef> images = loadAttachments(joinIds(ids));
        if (images.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "现场照片尚未上传成功，请重新上传后再试");
        }
        log.info("工人 AI 预检 workOrderNo={} assigneeId={} images={}",
                workOrder.getWorkOrderNo(), user.getUserId(), images.size());
        return requestAi(workOrder, content, images);
    }

    /**
     * 按整改前文字描述与 1 张整改后照片生成处理说明，只回填文本、不写工单。
     */
    public DraftGenerateResult generateDraft(Long workOrderId, DraftRequest request, LoginUser user) {
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new BizException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (workOrder.getAssigneeId() == null || !workOrder.getAssigneeId().equals(user.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        List<RectifyImageRef> images = loadAttachments(joinIds(request.getAttachmentIds()));
        if (images.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "整改后照片尚未上传成功，请重新上传后再试");
        }
        EventSnapshot snapshot = loadSnapshot(workOrder.getEventId());
        com.city.platform.common.api.dto.DraftGenerateRequest aiRequest =
                new com.city.platform.common.api.dto.DraftGenerateRequest(
                        workOrder.getWorkOrderNo(), workOrder.getEventNo(), workOrder.getCategoryCode(),
                        snapshot == null ? null : snapshot.getTitle(),
                        snapshot == null ? null : snapshot.getAddress(),
                        snapshot == null ? null : snapshot.getDescription(),
                        images.get(0));
        try {
            R<DraftGenerateResult> response = aiClient.generateDraft(aiRequest);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                throw new BizException(ErrorCode.AI_UNAVAILABLE,
                        response == null ? "AI 服务无响应" : response.getMessage());
            }
            log.info("生成处理说明 workOrderNo={} degraded={}",
                    workOrder.getWorkOrderNo(), response.getData().isDegraded());
            return response.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务不可用：" + e.getMessage());
        }
    }

    private WorkOrderLog latestSubmitLog(Long workOrderId) {
        return workOrderLogMapper.selectOne(Wrappers.<WorkOrderLog>lambdaQuery()
                .eq(WorkOrderLog::getWorkOrderId, workOrderId)
                .eq(WorkOrderLog::getAction, "SUBMIT")
                .orderByDesc(WorkOrderLog::getId)
                .last("LIMIT 1"));
    }

    private List<Long> parseIds(String attachmentIds) {
        if (!StringUtils.hasText(attachmentIds)) {
            return List.of();
        }
        return java.util.Arrays.stream(attachmentIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .toList();
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    /**
     * 提交结果后异步核实，失败只记日志，不影响工单提交。
     */
    public void verifyAsync(Long workOrderId) {
        CompletableFuture.runAsync(() -> {
            try {
                verify(workOrderId);
            } catch (Exception e) {
                log.warn("工单 AI 整改核实失败（不影响业务） workOrderId={}: {}", workOrderId, e.getMessage());
            }
        });
    }

    public RectifyVerifyResult parse(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RectifyVerifyResult.class);
        } catch (Exception e) {
            log.warn("解析 AI 核实结果失败: {}", e.getMessage());
            return null;
        }
    }

    private List<RectifyImageRef> loadAttachments(String attachmentIds) {
        if (!StringUtils.hasText(attachmentIds)) {
            return List.of();
        }
        try {
            R<List<RectifyImageRef>> response = eventClient.attachments(attachmentIds);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                return List.of();
            }
            return response.getData();
        } catch (Exception e) {
            log.warn("查询整改附件元数据失败 ids={}: {}", attachmentIds, e.getMessage());
            return List.of();
        }
    }

    private void save(Long workOrderId, RectifyVerifyResult result) {
        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        try {
            update.setAiVerifyResult(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("序列化 AI 核实结果失败: {}", e.getMessage());
            return;
        }
        update.setAiVerifyTime(LocalDateTime.now());
        workOrderMapper.updateById(update);
    }
}
