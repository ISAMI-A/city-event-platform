package com.city.platform.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.UserContext;
import com.city.platform.user.dto.WorkerCreateRequest;
import com.city.platform.user.dto.WorkerUpdateRequest;
import com.city.platform.user.entity.User;
import com.city.platform.user.entity.WorkerProfileEntity;
import com.city.platform.user.mapper.UserMapper;
import com.city.platform.user.mapper.WorkerProfileMapper;
import com.city.platform.user.vo.UserVO;
import com.city.platform.user.vo.WorkerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerManageService {

    private static final Map<String, String> DEPT_NAMES = Map.of(
            "ROAD", "市政道路班组",
            "LIGHTING", "路灯照明班组",
            "SANITATION", "环卫保洁班组",
            "FACILITY", "公共设施班组");
    private static final Set<String> ALLOWED_WORKER_STATUS = Set.of("ON_DUTY", "OFF_DUTY");
    private static final Set<String> ALLOWED_WORKER_TYPES = Set.of("LEADER", "MEMBER");

    private final UserMapper userMapper;
    private final WorkerProfileMapper workerProfileMapper;
    private final PasswordEncoder passwordEncoder;

    public List<WorkerVO> list(String workerType) {
        List<WorkerVO> workers = userMapper.selectWorkers();
        if (StringUtils.hasText(workerType)) {
            workers = workers.stream()
                    .filter(worker -> workerType.equalsIgnoreCase(worker.getWorkerType()))
                    .toList();
        }
        workers.forEach(worker -> worker.setPhone(UserVO.maskPhone(worker.getPhone())));
        return workers;
    }

    /**
     * 组长查看自己的组员。
     */
    public List<WorkerVO> myTeam() {
        Long leaderId = currentUserId();
        WorkerVO leader = userMapper.selectWorker(leaderId);
        if (leader == null || !"LEADER".equals(leader.getWorkerType())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅组长可查看团队");
        }
        List<WorkerVO> members = userMapper.selectWorkers().stream()
                .filter(worker -> leaderId.equals(worker.getLeaderId()))
                .toList();
        members.forEach(worker -> worker.setPhone(UserVO.maskPhone(worker.getPhone())));
        return members;
    }

    @Transactional
    public WorkerVO create(WorkerCreateRequest request) {
        String username = request.getUsername().trim();
        String deptCode = request.getDeptCode().trim();
        String deptName = DEPT_NAMES.get(deptCode);
        if (deptName == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "部门编码只允许 ROAD/LIGHTING/SANITATION/FACILITY");
        }
        String workerType = StringUtils.hasText(request.getWorkerType())
                ? request.getWorkerType().trim().toUpperCase() : "MEMBER";
        validateHierarchy(workerType, request.getLeaderId(), deptCode, null);
        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        user.setStatus("ENABLED");
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在");
        }
        userMapper.insertUserRole(user.getId(), RoleConstants.WORKER);

        WorkerProfileEntity profile = new WorkerProfileEntity();
        profile.setUserId(user.getId());
        profile.setWorkerType(workerType);
        profile.setLeaderId("LEADER".equals(workerType) ? null : request.getLeaderId());
        profile.setDeptCode(deptCode);
        profile.setDeptName(deptName);
        profile.setAreaCode(StringUtils.hasText(request.getAreaCode()) ? request.getAreaCode().trim() : null);
        profile.setWorkZone(StringUtils.hasText(request.getWorkZone()) ? request.getWorkZone().trim() : null);
        profile.setZoneLng(request.getZoneLng());
        profile.setZoneLat(request.getZoneLat());
        profile.setStatus("ON_DUTY");
        workerProfileMapper.insert(profile);

        log.info("创建处理人员 operator={} userId={} username={} dept={} type={}",
                currentUserId(), user.getId(), username, deptCode, workerType);
        return WorkerVO.of(user, profile);
    }

    @Transactional
    public WorkerVO update(Long id, WorkerUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "处理人员不存在");
        }
        if (!userMapper.selectRoleCodes(id).contains(RoleConstants.WORKER)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "目标用户不是处理人员");
        }
        WorkerProfileEntity profile = workerProfileMapper.selectOne(Wrappers.<WorkerProfileEntity>lambdaQuery()
                .eq(WorkerProfileEntity::getUserId, id).last("LIMIT 1"));
        if (profile == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "处理人员档案不存在");
        }

        if (StringUtils.hasText(request.getNickname())) {
            User userUpdate = new User();
            userUpdate.setId(id);
            userUpdate.setNickname(request.getNickname().trim());
            userMapper.updateById(userUpdate);
        }

        WorkerProfileEntity profileUpdate = new WorkerProfileEntity();
        profileUpdate.setId(profile.getId());
        boolean changed = false;
        String deptCode = profile.getDeptCode();
        if (StringUtils.hasText(request.getDeptCode())) {
            deptCode = request.getDeptCode().trim();
            String deptName = DEPT_NAMES.get(deptCode);
            if (deptName == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "部门编码只允许 ROAD/LIGHTING/SANITATION/FACILITY");
            }
            profileUpdate.setDeptCode(deptCode);
            profileUpdate.setDeptName(deptName);
            changed = true;
        }
        String workerType = StringUtils.hasText(request.getWorkerType())
                ? request.getWorkerType().trim().toUpperCase() : profile.getWorkerType();
        if (!ALLOWED_WORKER_TYPES.contains(workerType)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "人员类型只允许 LEADER 或 MEMBER");
        }
        if (!workerType.equals(profile.getWorkerType())) {
            if ("MEMBER".equals(workerType)) {
                Long members = workerProfileMapper.selectCount(Wrappers.<WorkerProfileEntity>lambdaQuery()
                        .eq(WorkerProfileEntity::getLeaderId, id));
                if (members != null && members > 0) {
                    throw new BizException(ErrorCode.STATE_CONFLICT, "该组长名下还有组员，不能改为组员");
                }
            }
            profileUpdate.setWorkerType(workerType);
            changed = true;
        }
        Long leaderId = request.getLeaderId() == null ? profile.getLeaderId() : request.getLeaderId();
        if (request.getLeaderId() != null && request.getLeaderId() == 0L) {
            leaderId = null;
        }
        if ("LEADER".equals(workerType)) {
            leaderId = null;
        }
        validateHierarchy(workerType, leaderId, deptCode, id);
        if (!java.util.Objects.equals(leaderId, profile.getLeaderId())) {
            profileUpdate.setLeaderId(leaderId);
            changed = true;
            // 组长没有上级，需要显式置空
            if (leaderId == null) {
                workerProfileMapper.update(null, Wrappers.<WorkerProfileEntity>lambdaUpdate()
                        .eq(WorkerProfileEntity::getId, profile.getId())
                        .set(WorkerProfileEntity::getLeaderId, null));
            }
        }
        if (StringUtils.hasText(request.getAreaCode())) {
            profileUpdate.setAreaCode(request.getAreaCode().trim());
            changed = true;
        }
        if (StringUtils.hasText(request.getWorkZone())) {
            profileUpdate.setWorkZone(request.getWorkZone().trim());
            changed = true;
        }
        if (request.getZoneLng() != null) {
            profileUpdate.setZoneLng(request.getZoneLng());
            changed = true;
        }
        if (request.getZoneLat() != null) {
            profileUpdate.setZoneLat(request.getZoneLat());
            changed = true;
        }
        if (StringUtils.hasText(request.getStatus())) {
            String status = request.getStatus().trim();
            if (!ALLOWED_WORKER_STATUS.contains(status)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "档案状态只允许 ON_DUTY 或 OFF_DUTY");
            }
            profileUpdate.setStatus(status);
            changed = true;
        }
        if (changed) {
            workerProfileMapper.updateById(profileUpdate);
        }

        log.info("更新处理人员 operator={} userId={}", currentUserId(), id);
        WorkerVO vo = userMapper.selectWorker(id);
        if (vo == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "处理人员档案不存在");
        }
        vo.setPhone(UserVO.maskPhone(vo.getPhone()));
        return vo;
    }

    public Map<String, Object> currentWorkerProfile() {
        Long userId = currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        WorkerProfileEntity profile = workerProfileMapper.selectOne(Wrappers.<WorkerProfileEntity>lambdaQuery()
                .eq(WorkerProfileEntity::getUserId, userId).last("LIMIT 1"));
        if (profile == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "未找到处理人员档案，请联系管理员维护");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("phone", UserVO.maskPhone(user.getPhone()));
        result.put("roles", userMapper.selectRoleCodes(userId));
        result.put("deptCode", profile.getDeptCode());
        result.put("deptName", profile.getDeptName());
        result.put("areaCode", profile.getAreaCode());
        result.put("workerType", profile.getWorkerType());
        result.put("leaderId", profile.getLeaderId());
        result.put("workZone", profile.getWorkZone());
        result.put("zoneLng", profile.getZoneLng());
        result.put("zoneLat", profile.getZoneLat());
        if (profile.getLeaderId() != null) {
            User leader = userMapper.selectById(profile.getLeaderId());
            result.put("leaderName", leader == null ? null : leader.getNickname());
        }
        if ("LEADER".equals(profile.getWorkerType())) {
            Long teamSize = workerProfileMapper.selectCount(Wrappers.<WorkerProfileEntity>lambdaQuery()
                    .eq(WorkerProfileEntity::getLeaderId, userId));
            result.put("teamSize", teamSize == null ? 0 : teamSize);
        }
        result.put("status", profile.getStatus());
        result.put("accountStatus", user.getStatus());
        return result;
    }

    private void validateHierarchy(String workerType, Long leaderId, String deptCode, Long selfId) {
        if ("LEADER".equals(workerType)) {
            if (leaderId != null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "组长不能设置所属组长");
            }
            return;
        }
        if (leaderId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "组员必须选择所属组长");
        }
        if (selfId != null && selfId.equals(leaderId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不能把自己设为所属组长");
        }
        WorkerVO leader = userMapper.selectWorker(leaderId);
        if (leader == null || !"LEADER".equals(leader.getWorkerType())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "所属组长不存在或不是组长");
        }
        if (StringUtils.hasText(deptCode) && !deptCode.equals(leader.getDeptCode())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "组员与组长必须属于同一部门");
        }
    }

    private Long currentUserId() {
        return UserContext.get() != null ? UserContext.get().getUserId() : StpUtil.getLoginIdAsLong();
    }
}
