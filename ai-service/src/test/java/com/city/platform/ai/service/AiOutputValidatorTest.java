package com.city.platform.ai.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiOutputValidatorTest {

    private final AiOutputValidator validator = new AiOutputValidator();

    @Test
    void shouldAcceptValidJsonWithCodeFence() {
        String raw = "```json\n{\"category\":\"PUBLIC_LIGHTING\",\"summary\":\"路灯不亮\","
                + "\"priority\":\"HIGH\",\"suggestion\":\"安排检修\",\"confidence\":0.91}\n```";
        AiOutputValidator.Result result = validator.validate(raw);

        assertTrue(result.isParsedOk());
        assertFalse(result.isDegraded());
        assertEquals("PUBLIC_LIGHTING", result.getCategoryCode());
        assertEquals("HIGH", result.getPriority());
        assertEquals(0, result.getConfidence().compareTo(new java.math.BigDecimal("0.91")));
    }

    @Test
    void shouldFallbackCategoryWhenInvalid() {
        String raw = "{\"category\":\"UNKNOWN_TYPE\",\"summary\":\"描述\",\"priority\":\"HIGH\","
                + "\"suggestion\":\"处理\",\"confidence\":2}";
        AiOutputValidator.Result result = validator.validate(raw);

        assertEquals("OTHER", result.getCategoryCode());
        assertEquals("HIGH", result.getPriority());
        assertEquals(0, result.getConfidence().compareTo(java.math.BigDecimal.ONE));
        assertTrue(result.getReason().contains("invalid_category"));
    }

    @Test
    void shouldFallbackPriorityWhenInvalid() {
        String raw = "{\"category\":\"ROAD_FACILITY\",\"priority\":\"CRITICAL\"}";
        AiOutputValidator.Result result = validator.validate(raw);

        assertEquals("MEDIUM", result.getPriority());
        assertTrue(result.getReason().contains("invalid_priority"));
    }

    @Test
    void shouldDegradeWhenNotJson() {
        AiOutputValidator.Result result = validator.validate("抱歉，我无法完成该请求");

        assertTrue(result.isDegraded());
        assertEquals("OTHER", result.getCategoryCode());
        assertEquals("MEDIUM", result.getPriority());
        assertFalse(result.isParsedOk());
    }

    @Test
    void shouldTruncateAndStripHtml() {
        String longSummary = "<b>摘要</b>" + "长".repeat(100);
        String raw = "{\"category\":\"ROAD_FACILITY\",\"summary\":\"" + longSummary
                + "\",\"priority\":\"LOW\",\"confidence\":0.5}";
        AiOutputValidator.Result result = validator.validate(raw);

        assertFalse(result.getSummary().contains("<b>"));
        assertEquals(60, result.getSummary().length());
    }
}
