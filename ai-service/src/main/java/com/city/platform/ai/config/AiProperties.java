package com.city.platform.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 大模型配置：Key 必须由环境变量注入，禁止硬编码。
 */
@Data
@ConfigurationProperties(prefix = "city.llm")
public class AiProperties {

    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
    private String apiKey = "";
    /** 文本分析模型 */
    private String model = "glm-4-flash";
    /** 视觉核实模型 */
    private String visionModel = "glm-4.6v-flash";
    /** 视觉模型拥堵/不可用时的回退模型 */
    private String visionFallbackModel = "glm-4v-flash";
    private int timeoutSeconds = 30;
}
