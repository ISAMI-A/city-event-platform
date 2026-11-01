package com.city.platform.common.api.feign;

import com.city.platform.common.api.dto.PingResult;
import com.city.platform.common.api.dto.WorkerProfile;
import com.city.platform.common.core.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * user-service 内部只读契约。url 为空时走 Nacos 服务发现，local 环境可指定静态地址。
 */
@FeignClient(name = "user-service", contextId = "userClient", path = "/internal/v1",
        url = "${city.feign.user-service-url:}")
public interface UserClient {

    @GetMapping("/system/ping")
    R<PingResult> ping();

    @GetMapping("/users/{id}/worker-profile")
    R<WorkerProfile> workerProfile(@PathVariable("id") Long id);

    @GetMapping("/workers/leaders")
    R<List<WorkerProfile>> leaders(@RequestParam("areaCode") String areaCode);

    @GetMapping("/workers/members")
    R<List<WorkerProfile>> members(@RequestParam("leaderId") Long leaderId);
}
