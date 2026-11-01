package com.city.platform.common.api.feign;

import com.city.platform.common.api.dto.AiAnalysisResult;
import com.city.platform.common.api.dto.DraftGenerateRequest;
import com.city.platform.common.api.dto.DraftGenerateResult;
import com.city.platform.common.api.dto.EventReviewRequest;
import com.city.platform.common.api.dto.EventReviewResult;
import com.city.platform.common.api.dto.RectifyVerifyRequest;
import com.city.platform.common.api.dto.RectifyVerifyResult;
import com.city.platform.common.core.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * ai-service 内部分析结果查询契约。
 */
@FeignClient(name = "ai-service", contextId = "aiClient", path = "/internal/v1/ai",
        url = "${city.feign.ai-service-url:}")
public interface AiClient {

    @GetMapping("/analyses/{callId}")
    R<AiAnalysisResult> getAnalysis(@PathVariable("callId") Long callId);

    /**
     * 视觉整改核实：根据整改说明与现场照片判断整改是否完成。
     */
    @PostMapping("/verify-rectify")
    R<RectifyVerifyResult> verifyRectify(@RequestBody RectifyVerifyRequest request);

    /**
     * 按事件描述与 1 张整改后照片生成处理说明草稿。
     */
    @PostMapping("/generate-draft")
    R<DraftGenerateResult> generateDraft(@RequestBody DraftGenerateRequest request);

    /**
     * 事件紧急度复评：管理员审核前由大模型重新评估紧急程度。
     */
    @PostMapping("/review-event")
    R<EventReviewResult> reviewEvent(@RequestBody EventReviewRequest request);
}
