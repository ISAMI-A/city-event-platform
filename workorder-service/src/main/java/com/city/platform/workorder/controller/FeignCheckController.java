package com.city.platform.workorder.controller;

import com.city.platform.common.api.dto.PingResult;
import com.city.platform.common.api.feign.UserClient;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import cn.dev33.satoken.annotation.SaCheckRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OpenFeign 真实调用验证：workorder-service -> user-service（内部接口）。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FeignCheckController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserClient userClient;

    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    @GetMapping("/api/v1/system/feign-check")
    @SaCheckRole(RoleConstants.ADMIN)
    public R<Map<String, Object>> feignCheck() {
        return R.ok(invokeUserService());
    }

    @GetMapping("/internal/v1/system/feign-check")
    public R<Map<String, Object>> internalFeignCheck() {
        return R.ok(invokeUserService());
    }

    private Map<String, Object> invokeUserService() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> local = new LinkedHashMap<>();
        local.put("service", "workorder-service");
        local.put("instance", instanceId);
        local.put("timestamp", LocalDateTime.now().format(FORMATTER));
        result.put("local", local);

        try {
            R<PingResult> remote = userClient.ping();
            if (remote == null || !remote.isSuccess()) {
                throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "调用 user-service 失败");
            }
            result.put("remote", remote.getData());
            result.put("traceId", remote.getTraceId());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("Feign 调用 user-service 失败", e);
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "调用 user-service 异常：" + e.getMessage());
        }
        return result;
    }
}
