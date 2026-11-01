package com.city.platform.ai.controller;

import com.city.platform.ai.dto.AiAnalyzeRequest;
import com.city.platform.ai.service.AiAnalysisService;
import com.city.platform.ai.vo.AiAnalyzeResponse;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.context.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/analyze")
    public R<AiAnalyzeResponse> analyze(@Valid @RequestBody AiAnalyzeRequest request) {
        return R.ok(aiAnalysisService.analyze(request, UserContext.requireUserId()));
    }
}
