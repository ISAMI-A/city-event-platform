package com.city.platform.ai.controller;

import com.city.platform.ai.service.AiAnalysisService;
import com.city.platform.ai.service.EventReviewService;
import com.city.platform.ai.service.RectifyVerifyService;
import com.city.platform.common.api.dto.AiAnalysisResult;
import com.city.platform.common.api.dto.DraftGenerateRequest;
import com.city.platform.common.api.dto.DraftGenerateResult;
import com.city.platform.common.api.dto.EventReviewRequest;
import com.city.platform.common.api.dto.EventReviewResult;
import com.city.platform.common.api.dto.RectifyVerifyRequest;
import com.city.platform.common.api.dto.RectifyVerifyResult;
import com.city.platform.common.core.model.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/ai")
@RequiredArgsConstructor
public class InternalAiController {

    private final AiAnalysisService aiAnalysisService;
    private final RectifyVerifyService rectifyVerifyService;
    private final EventReviewService eventReviewService;

    @GetMapping("/analyses/{callId}")
    public R<AiAnalysisResult> getAnalysis(@PathVariable("callId") Long callId) {
        return R.ok(aiAnalysisService.getAnalysis(callId));
    }

    /**
     * 视觉整改核实（工单服务调用）。
     */
    @PostMapping("/verify-rectify")
    public R<RectifyVerifyResult> verifyRectify(@RequestBody RectifyVerifyRequest request) {
        return R.ok(rectifyVerifyService.verify(request));
    }

    /**
     * 按事件描述与整改后照片生成处理说明草稿（工单服务调用）。
     */
    @PostMapping("/generate-draft")
    public R<DraftGenerateResult> generateDraft(@RequestBody DraftGenerateRequest request) {
        return R.ok(rectifyVerifyService.generateDraft(request));
    }

    /**
     * 事件紧急度复评（事件服务调用）。
     */
    @PostMapping("/review-event")
    public R<EventReviewResult> reviewEvent(@RequestBody EventReviewRequest request) {
        return R.ok(eventReviewService.review(request));
    }
}
