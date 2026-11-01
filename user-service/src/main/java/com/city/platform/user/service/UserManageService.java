package com.city.platform.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.PageQuery;
import com.city.platform.common.core.model.PageResult;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.UserContext;
import com.city.platform.user.dto.ResetPasswordRequest;
import com.city.platform.user.dto.UserRolesRequest;
import com.city.platform.user.dto.UserStatusRequest;
import com.city.platform.user.entity.User;
import com.city.platform.user.entity.WorkerProfileEntity;
import com.city.platform.user.mapper.UserMapper;
import com.city.platform.user.mapper.WorkerProfileMapper;
import com.city.platform.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManageService {

    private static final Set<String> ALLOWED_ROLES =
            Set.of(RoleConstants.RESIDENT, RoleConstants.WORKER, RoleConstants.ADMIN);
    private static final Set<String> ALLOWED_STATUS = Set.of("ENABLED", "DISABLED");

    private final UserMapper userMapper;
    private final WorkerProfileMapper workerProfileMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(PageQuery query, String keyword, String status, String role) {
        if (StringUtils.hasText(status) && !ALLOWED_STATUS.contains(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "状态只允许 ENABLED 或 DISABLED");
        }
        if (StringUtils.hasText(role) && !ALLOWED_ROLES.contains(role)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "角色只允许 RESIDENT/WORKER/ADMIN");
        }
        var wrapper = Wrappers.<User>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getNickname, keyword)
                    .or().like(User::getUsername, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(User::getStatus, status);
        }
        if (StringUtils.hasText(role)) {
            wrapper.apply("EXISTS (SELECT 1 FROM t_user_role r WHERE r.user_id = t_user.id AND r.role_code = {0})", role);
        }
        wrapper.orderByDesc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(query.getPage(), query.getSize()), wrapper);
        IPage<UserVO> voPage = page.convert(UserVO::from);
        fillRoles(voPage.getRecords());
        return PageResult.of(voPage);
    }

    public UserVO detail(Long id) {
        User user = requireUser(id);
        UserVO vo = UserVO.from(user);
        vo.setRoles(userMapper.selectRoleCodes(id));
        WorkerProfileEntity profile = workerProfileMapper.selectOne(Wrappers.<WorkerProfileEntity>lambdaQuery()
                .eq(WorkerProfileEntity::getUserId, id).last("LIMIT 1"));
        if (profile != null) {
            vo.setWorkerProfile(UserVO.WorkerProfileVO.from(profile));
        }
        return vo;
    }

    public UserVO updateStatus(Long id, UserStatusRequest request) {
        String status = request.getStatus().trim();
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "状态只允许 ENABLED 或 DISABLED");
        }
        assertNotSelf(id, "不能修改自己的账号状态");
        User user = requireUser(id);
        User update = new User();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        log.info("修改用户状态 operator={} userId={} {} -> {}", currentUserId(), id, user.getStatus(), status);
        return detail(id);
    }

    @Transactional
    public UserVO updateRoles(Long id, UserRolesRequest request) {
        List<String> roles = request.getRoles().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (roles.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "角色不能为空");
        }
        if (!ALLOWED_ROLES.containsAll(roles)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "角色只允许 RESIDENT/WORKER/ADMIN");
        }
        assertNotSelf(id, "不能修改自己的角色");
        requireUser(id);
        if (roles.contains(RoleConstants.WORKER)) {
            WorkerProfileEntity profile = workerProfileMapper.selectOne(Wrappers.<WorkerProfileEntity>lambdaQuery()
                    .eq(WorkerProfileEntity::getUserId, id).last("LIMIT 1"));
            if (profile == null) {
                throw new BizException(ErrorCode.STATE_CONFLICT, "请先在工人管理中维护处理人员档案");
            }
        }
        userMapper.deleteUserRoles(id);
        roles.forEach(role -> userMapper.insertUserRole(id, role));
        log.info("修改用户角色 operator={} userId={} roles={}", currentUserId(), id, roles);
        return detail(id);
    }

    public void resetPassword(Long id, ResetPasswordRequest request) {
        assertNotSelf(id, "不能重置自己的密码，请通过个人资料修改");
        requireUser(id);
        User update = new User();
        update.setId(id);
        update.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);
        log.info("重置用户密码 operator={} userId={}", currentUserId(), id);
    }

    private void fillRoles(List<UserVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream().map(UserVO::getId).toList();
        Map<Long, List<String>> roleMap = userMapper.selectRoleItems(userIds).stream()
                .collect(Collectors.groupingBy(UserMapper.UserRoleItem::getUserId,
                        Collectors.mapping(UserMapper.UserRoleItem::getRoleCode, Collectors.toList())));
        records.forEach(vo -> vo.setRoles(roleMap.getOrDefault(vo.getId(), List.of())));
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void assertNotSelf(Long id, String message) {
        if (id.equals(currentUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN, message);
        }
    }

    private Long currentUserId() {
        return UserContext.get() != null ? UserContext.get().getUserId() : StpUtil.getLoginIdAsLong();
    }
}
