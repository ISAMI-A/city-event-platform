package com.city.platform.user.controller;

import com.city.platform.common.core.model.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 服务存活探测：用于网关路由与 Feign 真实调用验证。
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${spring.application.name:user-service}")
    private String applicationName;

    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        return R.ok(payload());
    }

    private Map<String, Object> payload() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", applicationName);
        body.put("instance", instanceId);
        body.put("timestamp", LocalDateTime.now().format(FORMATTER));
        body.put("traceId", org.slf4j.MDC.get("traceId"));
        return body;
    }
}
