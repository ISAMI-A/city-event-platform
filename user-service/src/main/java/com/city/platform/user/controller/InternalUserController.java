package com.city.platform.user.controller;

import com.city.platform.common.api.dto.WorkerProfile;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.user.entity.User;
import com.city.platform.user.entity.WorkerProfileEntity;
import com.city.platform.user.mapper.UserMapper;
import com.city.platform.user.mapper.WorkerProfileMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 内部接口：仅允许携带 X-Internal-Token 的服务间调用。
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalUserController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final WorkerProfileMapper workerProfileMapper;

    @Value("${spring.application.name:user-service}")
    private String applicationName;

    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    @GetMapping("/system/ping")
    public R<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", applicationName);
        body.put("instance", instanceId);
        body.put("timestamp", LocalDateTime.now().format(FORMATTER));
        body.put("traceId", MDC.get("traceId"));
        return R.ok(body);
    }

    @GetMapping("/users/{id}/worker-profile")
    public R<WorkerProfile> workerProfile(@PathVariable("id") Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "处理人员不存在");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        List<String> roles = userMapper.selectRoleCodes(id);
        if (!roles.contains(RoleConstants.WORKER)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "目标用户不是处理人员");
        }
        WorkerProfileEntity profile = workerProfileMapper.selectOne(Wrappers.<WorkerProfileEntity>lambdaQuery()
                .eq(WorkerProfileEntity::getUserId, id).last("LIMIT 1"));
        if (profile == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "处理人员档案不存在");
        }
        if (!"ON_DUTY".equals(profile.getStatus())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "处理人员当前不在岗");
        }
        return R.ok(toProfile(user, profile));
    }

    /**
     * 按片区查询在岗组长：事件自动派单时用于匹配责任组长。
     */
    @GetMapping("/workers/leaders")
    public R<List<WorkerProfile>> leaders(@RequestParam("areaCode") String areaCode) {
        List<WorkerProfileEntity> profiles = workerProfileMapper.selectList(Wrappers.<WorkerProfileEntity>lambdaQuery()
                .eq(WorkerProfileEntity::getWorkerType, "LEADER")
                .eq(WorkerProfileEntity::getStatus, "ON_DUTY")
                .eq(WorkerProfileEntity::getAreaCode, areaCode)
                .orderByAsc(WorkerProfileEntity::getId));
        return R.ok(profiles.stream().map(profile -> toProfile(userMapper.selectById(profile.getUserId()), profile)).toList());
    }

    /**
     * 按组长查询在岗组员：组长发布注意事项、转达提醒时使用。
     */
    @GetMapping("/workers/members")
    public R<List<WorkerProfile>> members(@RequestParam("leaderId") Long leaderId) {
        List<WorkerProfileEntity> profiles = workerProfileMapper.selectList(Wrappers.<WorkerProfileEntity>lambdaQuery()
                .eq(WorkerProfileEntity::getWorkerType, "MEMBER")
                .eq(WorkerProfileEntity::getLeaderId, leaderId)
                .eq(WorkerProfileEntity::getStatus, "ON_DUTY")
                .orderByAsc(WorkerProfileEntity::getId));
        return R.ok(profiles.stream().map(profile -> toProfile(userMapper.selectById(profile.getUserId()), profile)).toList());
    }

    private WorkerProfile toProfile(User user, WorkerProfileEntity profile) {
        WorkerProfile dto = new WorkerProfile();
        dto.setUserId(profile.getUserId());
        dto.setNickname(user == null ? "" : user.getNickname());
        dto.setDeptCode(profile.getDeptCode());
        dto.setDeptName(profile.getDeptName());
        dto.setStatus(profile.getStatus());
        dto.setWorkerType(profile.getWorkerType());
        dto.setLeaderId(profile.getLeaderId());
        dto.setAreaCode(profile.getAreaCode());
        dto.setWorkZone(profile.getWorkZone());
        return dto;
    }
}
