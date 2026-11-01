package com.city.platform.ai.service;

import java.util.List;

/**
 * 大模型客户端抽象，便于切换供应商与测试替换。
 */
public interface LlmClient {

    String chat(String systemPrompt, String userPrompt);

    /**
     * 多模态调用：图片以 data URI（base64）传入；model 允许主模型/回退模型切换。
     */
    String chatWithImages(String model, String systemPrompt, String userPrompt, List<String> imageDataUris);
}
