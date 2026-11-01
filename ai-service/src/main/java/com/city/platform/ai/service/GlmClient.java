package com.city.platform.ai.service;

import com.city.platform.ai.config.AiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 智谱 GLM（OpenAI 兼容协议）客户端。
 */
@Slf4j
@Component
public class GlmClient implements LlmClient {

    private final RestClient restClient;
    private final AiProperties properties;
    private final Environment environment;

    public GlmClient(RestClient llmRestClient, AiProperties properties, Environment environment) {
        this.restClient = llmRestClient;
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return post(properties.getModel(), systemPrompt, userPrompt, 512);
    }

    @Override
    public String chatWithImages(String model, String systemPrompt, String userPrompt, List<String> imageDataUris) {
        List<Map<String, Object>> content = new ArrayList<>();
        for (String dataUri : imageDataUris) {
            content.add(Map.of("type", "image_url", "image_url", Map.of("url", dataUri)));
        }
        content.add(Map.of("type", "text", "text", userPrompt));
        return post(model, systemPrompt, content, 800);
    }

    private String post(String model, String systemPrompt, Object userContent, int maxTokens) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new IllegalStateException("大模型 API Key 未配置");
        }
        // 故障注入：用于验证 Sentinel 慢调用熔断，仅演示环境使用
        if (environment.getProperty("demo.fault.llm-slow", Boolean.class, false)) {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)),
                "temperature", 0.2,
                "max_tokens", maxTokens);

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException("大模型返回为空");
        }
        return response.path("choices").path(0).path("message").path("content").asText(null);
    }
}
