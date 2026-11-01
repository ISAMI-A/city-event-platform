package com.city.platform.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 从共享上传目录读取附件并转为视觉模型可用的 data URI。
 * 仅接受 jpeg/png，单张不超过 2MB，最多取 3 张，避免请求体过大。
 */
@Slf4j
@Component
public class VisionImageLoader {

    private static final int MAX_IMAGES = 3;
    private static final long MAX_IMAGE_BYTES = 2 * 1024 * 1024L;

    @Value("${city.file.root:data/uploads}")
    private String uploadRoot;

    public List<String> load(List<String> accessUrls) {
        if (accessUrls == null || accessUrls.isEmpty()) {
            return List.of();
        }
        Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
        List<String> result = new ArrayList<>();
        for (String url : accessUrls) {
            if (result.size() >= MAX_IMAGES) {
                break;
            }
            if (!StringUtils.hasText(url)) {
                continue;
            }
            try {
                String relative = url.startsWith("/uploads") ? url.substring("/uploads".length()) : url;
                Path file = root.resolve(relative.replaceFirst("^/+", "")).normalize();
                if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                    log.warn("AI 分析照片不存在 url={}", url);
                    continue;
                }
                if (Files.size(file) > MAX_IMAGE_BYTES) {
                    log.warn("AI 分析照片超过 2MB，跳过 url={} size={}", url, Files.size(file));
                    continue;
                }
                String mime = file.getFileName().toString().toLowerCase().endsWith(".png")
                        ? "image/png" : "image/jpeg";
                result.add("data:" + mime + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
            } catch (Exception e) {
                log.warn("读取 AI 分析照片失败 url={} error={}", url, e.getMessage());
            }
        }
        return result;
    }
}
