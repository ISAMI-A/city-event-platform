package com.city.platform.event.controller;

import com.city.platform.common.core.model.R;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${spring.application.name:event-service}")
    private String applicationName;

    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", applicationName);
        body.put("instance", instanceId);
        body.put("timestamp", LocalDateTime.now().format(FORMATTER));
        body.put("traceId", MDC.get("traceId"));
        return R.ok(body);
    }
}
