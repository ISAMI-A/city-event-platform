package com.city.platform.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

/**
 * AI 输出校验器：模型输出只作为建议，所有字段必须经过本类校验与收敛。
 */
public class AiOutputValidator {

    public static final Set<String> CATEGORIES = Set.of(
            "ROAD_FACILITY", "PUBLIC_LIGHTING", "ENVIRONMENT_SANITATION",
            "PUBLIC_FACILITY", "OTHER");

    public static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

    private static final int DESCRIPTION_MAX = 500;
    private static final int SUMMARY_MAX = 60;
    private static final int SUGGESTION_MAX = 200;
    private static final int REASONING_MAX = 200;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Result validate(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return Result.degraded("EMPTY_RESPONSE");
        }
        String cleaned = stripCodeFence(rawContent.trim());
        try {
            JsonNode node = objectMapper.readTree(cleaned);
            String category = text(node, "category");
            String priority = text(node, "priority");
            String description = sanitize(text(node, "description"), DESCRIPTION_MAX);
            String summary = sanitize(text(node, "summary"), SUMMARY_MAX);
            String suggestion = sanitize(text(node, "suggestion"), SUGGESTION_MAX);
            String reasoning = sanitize(text(node, "reasoning"), REASONING_MAX);
            BigDecimal confidence = clamp(objectMapper.convertValue(node.path("confidence"), BigDecimal.class));

            String reason = null;
            if (!CATEGORIES.contains(category)) {
                category = "OTHER";
                reason = "invalid_category";
            }
            if (!PRIORITIES.contains(priority)) {
                priority = "MEDIUM";
                reason = reason == null ? "invalid_priority" : reason + ",invalid_priority";
            }
            return new Result(category, description, summary, priority, suggestion, reasoning,
                    confidence, false, reason, true);
        } catch (Exception e) {
            return Result.degraded("PARSE_FAILED");
        }
    }

    private String stripCodeFence(String content) {
        String result = content;
        if (result.startsWith("```")) {
            int firstLineEnd = result.indexOf('\n');
            if (firstLineEnd > 0) {
                result = result.substring(firstLineEnd + 1);
            }
            if (result.endsWith("```")) {
                result = result.substring(0, result.length() - 3);
            }
        }
        return result.trim();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("<[^>]+>", "")
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                .trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    @Data
    @AllArgsConstructor
    public static class Result {
        private String categoryCode;
        private String description;
        private String summary;
        private String priority;
        private String suggestion;
        private String reasoning;
        private BigDecimal confidence;
        private boolean degraded;
        private String reason;
        private boolean parsedOk;

        public static Result degraded(String reason) {
            return new Result("OTHER", null, null, "MEDIUM", null, null,
                    BigDecimal.ZERO, true, reason, false);
        }
    }
}
