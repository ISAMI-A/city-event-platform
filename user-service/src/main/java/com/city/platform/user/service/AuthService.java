package com.city.platform.user.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.security.constant.RoleConstants;
import com.city.platform.common.security.context.UserContext;
import com.city.platform.user.client.WechatClient;
import com.city.platform.user.dto.LoginRequest;
import com.city.platform.user.dto.ProfileUpdateRequest;
import com.city.platform.user.dto.RegisterRequest;
import com.city.platform.user.dto.WxBindRequest;
import com.city.platform.user.dto.WxLoginRequest;
import com.city.platform.user.dto.WxRegisterRequest;
import com.city.platform.user.entity.User;
import com.city.platform.user.mapper.UserMapper;
import com.city.platform.user.vo.LoginVO;
import com.city.platform.user.vo.WxLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String WX_TICKET_PREFIX = "city:auth:wx-ticket:";
    private static final Duration WX_TICKET_TTL = Duration.ofMinutes(5);
    /** 自动生成账号用字符集：去掉易混淆的 0/o/1/l/i */
    private static final String ACCOUNT_CHARS = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final WechatClient wechatClient;
    private final EmailCodeService emailCodeService;

    public LoginVO login(LoginRequest request) {
        User user = findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        WxLoginVO issued = issueToken(user, request.getClient());
        return new LoginVO(issued.getToken(), issued.getUserId(), issued.getNickname(),
                issued.getActiveRole(), issued.getRoles(), issued.getExpireSeconds());
    }

    /**
     * 微信登录：已绑定直接签发 token；未绑定返回一次性绑定凭证，由前端选择绑定工人账号或注册居民账号。
     */
    public WxLoginVO wxLogin(WxLoginRequest request) {
        WechatClient.WxSession session = wechatClient.code2session(request.getCode());
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getOpenId, session.openId())
                .last("LIMIT 1"));
        if (user != null) {
            return issueToken(user, "miniprogram");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(ticketKey(ticket), session.openId(), WX_TICKET_TTL);
        log.info("微信首次登录，生成绑定凭证 openId={}", session.openId());
        return WxLoginVO.pending(ticket);
    }

    /**
     * 工人首次微信登录：校验账号密码后把 openid 绑定到该账号；不取消原密码登录方式。
     */
    public WxLoginVO wxBind(WxBindRequest request) {
        String openId = requireTicket(request.getBindTicket());
        User user = findByUsername(request.getUsername());
        if (user == null || !StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (StringUtils.hasText(user.getOpenId()) && !user.getOpenId().equals(openId)) {
            throw new BizException(ErrorCode.WECHAT_BIND_CONFLICT, "该账号已绑定其他微信");
        }
        if (!openId.equals(user.getOpenId())) {
            Long used = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getOpenId, openId));
            if (used != null && used > 0) {
                throw new BizException(ErrorCode.WECHAT_BIND_CONFLICT, "该微信已绑定其他账号");
            }
            User update = new User();
            update.setId(user.getId());
            update.setOpenId(openId);
            try {
                userMapper.updateById(update);
            } catch (DuplicateKeyException e) {
                throw new BizException(ErrorCode.WECHAT_BIND_CONFLICT, "该微信已绑定其他账号");
            }
        }
        redisTemplate.delete(ticketKey(request.getBindTicket()));
        log.info("微信绑定账号成功 userId={}", user.getId());
        return issueToken(user, "miniprogram");
    }

    /**
     * 居民首次微信登录：创建居民账号并绑定 RESIDENT 角色。
     */
    public WxLoginVO wxRegister(WxRegisterRequest request) {
        String openId = requireTicket(request.getBindTicket());
        User existing = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getOpenId, openId)
                .last("LIMIT 1"));
        if (existing != null) {
            redisTemplate.delete(ticketKey(request.getBindTicket()));
            return issueToken(existing, "miniprogram");
        }
        User user = new User();
        user.setOpenId(openId);
        user.setUsername(generateWxUsername());
        user.setNickname(defaultNickname(request.getNickname()));
        user.setStatus("ENABLED");
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.WECHAT_BIND_CONFLICT, "该微信已绑定其他账号");
        }
        userMapper.insertUserRole(user.getId(), RoleConstants.RESIDENT);
        redisTemplate.delete(ticketKey(request.getBindTicket()));
        log.info("微信注册居民账号成功 userId={}", user.getId());
        return issueToken(user, "miniprogram");
    }

    /**
     * 居民自助注册：创建账号并绑定 RESIDENT 角色，注册成功后直接签发 token。
     */
    @Transactional
    public LoginVO register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (findByUsername(username) != null) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "用户名已被占用，请换一个");
        }
        Long phoneUsed = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, request.getPhone().trim()));
        if (phoneUsed != null && phoneUsed > 0) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "该手机号已注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname().trim());
        user.setPhone(request.getPhone().trim());
        user.setStatus("ENABLED");
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "用户名已被占用，请换一个");
        }
        userMapper.insertUserRole(user.getId(), RoleConstants.RESIDENT);
        log.info("居民自助注册成功 userId={} username={}", user.getId(), username);
        WxLoginVO issued = issueToken(user, "miniprogram");
        return new LoginVO(issued.getToken(), issued.getUserId(), issued.getNickname(),
                issued.getActiveRole(), issued.getRoles(), issued.getExpireSeconds());
    }

    /**
     * 已登录账号绑定微信：用 wx.login 的 code 换 openid，一个微信只能绑定一个账号。
     */
    public Map<String, Object> bindWechat(String code) {
        Long userId = currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        WechatClient.WxSession session = wechatClient.code2session(code);
        String openId = session.openId();
        if (StringUtils.hasText(user.getOpenId())) {
            if (user.getOpenId().equals(openId)) {
                return wxBindState(true, "当前微信已绑定该账号");
            }
            throw new BizException(ErrorCode.WECHAT_BIND_CONFLICT, "该账号已绑定其他微信，请先解绑");
        }
        Long used = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getOpenId, openId));
        if (used != null && used > 0) {
            throw new BizException(ErrorCode.WECHAT_BIND_CONFLICT, "该微信已绑定其他账号");
        }
        User update = new User();
        update.setId(userId);
        update.setOpenId(openId);
        if (StringUtils.hasText(session.unionId())) {
            update.setUnionId(session.unionId());
        }
        try {
            userMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.WECHAT_BIND_CONFLICT, "该微信已绑定其他账号");
        }
        log.info("账号绑定微信成功 userId={}", userId);
        return wxBindState(true, "微信绑定成功");
    }

    /**
     * 解绑微信：必须已设置账号密码，避免解绑后无法登录。
     */
    public Map<String, Object> unbindWechat() {
        Long userId = currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!StringUtils.hasText(user.getOpenId())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "当前账号未绑定微信");
        }
        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "账号未设置密码，解绑后将无法登录，请先设置密码");
        }
        userMapper.clearWechatBinding(userId);
        log.info("账号解绑微信成功 userId={}", userId);
        return wxBindState(false, "微信已解绑");
    }

    private Map<String, Object> wxBindState(boolean bound, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("wxBound", bound);
        result.put("message", message);
        return result;
    }

    public Map<String, Object> updateProfile(ProfileUpdateRequest request) {
        Long userId = currentUserId();
        User update = new User();
        update.setId(userId);
        if (StringUtils.hasText(request.getNickname())) {
            update.setNickname(request.getNickname().trim());
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            update.setAvatarUrl(request.getAvatarUrl());
        }
        userMapper.updateById(update);
        return currentUser();
    }

    /**
     * 修改登录账号：全局唯一；已设置密码的账号需校验当前登录密码。
     */
    public Map<String, Object> updateUsername(String username, String password) {
        String value = username == null ? "" : username.trim();
        Long userId = currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (value.equals(user.getUsername())) {
            return currentUser();
        }
        if (StringUtils.hasText(user.getPasswordHash())) {
            requireCurrentPassword(user, password);
        }
        User existing = findByUsername(value);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "该账号已被占用，请更换");
        }
        User update = new User();
        update.setId(userId);
        update.setUsername(value);
        try {
            userMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "该账号已被占用，请更换");
        }
        log.info("用户修改登录账号 userId={} username={}", userId, value);
        return currentUser();
    }

    /**
     * 绑定 / 更换手机号：仅做 11 位格式与唯一性校验，不发短信。
     * 更换已绑定手机号时需校验当前登录密码。
     */
    public Map<String, Object> updatePhone(String phone, String password) {
        String value = phone == null ? "" : phone.trim();
        Long userId = currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (value.equals(user.getPhone())) {
            return currentUser();
        }
        if (StringUtils.hasText(user.getPhone())) {
            if (!StringUtils.hasText(user.getPasswordHash())) {
                throw new BizException(ErrorCode.STATE_CONFLICT, "请先设置登录密码后再更换手机号");
            }
            requireCurrentPassword(user, password);
        }
        Long used = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, value)
                .ne(User::getId, userId));
        if (used != null && used > 0) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "该手机号已被其他账号绑定");
        }
        User update = new User();
        update.setId(userId);
        update.setPhone(value);
        userMapper.updateById(update);
        log.info("用户绑定手机号 userId={}", userId);
        return currentUser();
    }

    /**
     * 设置 / 修改登录密码：已设置过密码时必须校验原密码。
     */
    public Map<String, Object> changePassword(String oldPassword, String newPassword) {
        Long userId = currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (StringUtils.hasText(user.getPasswordHash())) {
            if (!StringUtils.hasText(oldPassword)
                    || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                throw new BizException(ErrorCode.BAD_CREDENTIALS, "原密码不正确");
            }
        }
        User update = new User();
        update.setId(userId);
        update.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
        log.info("用户设置登录密码 userId={}", userId);
        return currentUser();
    }

    public void sendEmailCode(String email, String ip) {
        emailCodeService.send(email, ip);
    }

    /**
     * 绑定 / 更换邮箱：验证码校验通过后写入邮箱并标记已验证；一个邮箱只能绑定一个账号。
     * 更换已绑定邮箱时需校验当前登录密码。
     */
    public Map<String, Object> bindEmail(String email, String code, String password) {
        String target = EmailCodeService.normalize(email);
        if (!EmailCodeService.validEmail(target)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "邮箱格式不正确");
        }
        Long userId = currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (target.equals(user.getEmail())) {
            return currentUser();
        }
        if (StringUtils.hasText(user.getEmail())) {
            if (!StringUtils.hasText(user.getPasswordHash())) {
                throw new BizException(ErrorCode.STATE_CONFLICT, "请先设置登录密码后再更换邮箱");
            }
            requireCurrentPassword(user, password);
        }
        Long used = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, target)
                .ne(User::getId, userId));
        if (used != null && used > 0) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "该邮箱已被其他账号绑定");
        }
        emailCodeService.verify(target, code);
        User update = new User();
        update.setId(userId);
        update.setEmail(target);
        update.setEmailVerified(1);
        try {
            userMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.USER_ALREADY_EXISTS, "该邮箱已被其他账号绑定");
        }
        log.info("用户绑定邮箱 userId={} email={}", userId, target);
        return currentUser();
    }

    public Map<String, Object> unbindEmail() {
        Long userId = currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BizException(ErrorCode.STATE_CONFLICT, "当前账号未绑定邮箱");
        }
        userMapper.clearEmail(userId);
        log.info("用户解绑邮箱 userId={}", userId);
        return currentUser();
    }

    /**
     * 校验当前登录密码（更换已绑定手机号/邮箱时使用）。
     */
    private void requireCurrentPassword(User user, String password) {
        if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS, "当前登录密码不正确");
        }
    }

    public Map<String, Object> currentUser() {
        Long userId = currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("phone", com.city.platform.user.vo.UserVO.maskPhone(user.getPhone()));
        result.put("email", user.getEmail());
        result.put("emailVerified", Integer.valueOf(1).equals(user.getEmailVerified()));
        result.put("hasPassword", StringUtils.hasText(user.getPasswordHash()));
        result.put("wxBound", StringUtils.hasText(user.getOpenId()));
        result.put("activeRole", StpUtil.getSession().get("activeRole"));
        result.put("roles", userMapper.selectRoleCodes(userId));
        return result;
    }

    public void logout() {
        StpUtil.logout();
    }

    private Long currentUserId() {
        return UserContext.get() != null ? UserContext.get().getUserId() : StpUtil.getLoginIdAsLong();
    }

    private User findByUsername(String username) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    private String requireTicket(String ticket) {
        String openId = redisTemplate.opsForValue().get(ticketKey(ticket));
        if (!StringUtils.hasText(openId)) {
            throw new BizException(ErrorCode.WECHAT_TICKET_INVALID);
        }
        return openId;
    }

    private String ticketKey(String ticket) {
        return WX_TICKET_PREFIX + ticket;
    }

    /**
     * 微信快捷登录自动生成唯一登录账号：wx_ + 6 位随机字符，冲突时重试。
     */
    private String generateWxUsername() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder("wx_");
            for (int j = 0; j < 6; j++) {
                sb.append(ACCOUNT_CHARS.charAt(RANDOM.nextInt(ACCOUNT_CHARS.length())));
            }
            String candidate = sb.toString();
            if (findByUsername(candidate) == null) {
                return candidate;
            }
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "账号生成失败，请重试");
    }

    private String defaultNickname(String nickname) {
        if (StringUtils.hasText(nickname)) {
            return nickname.trim();
        }
        StringBuilder sb = new StringBuilder("居民_");
        for (int i = 0; i < 4; i++) {
            sb.append(ACCOUNT_CHARS.charAt(RANDOM.nextInt(ACCOUNT_CHARS.length())));
        }
        return sb.toString();
    }

    private WxLoginVO issueToken(User user, String client) {
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        List<String> roles = userMapper.selectRoleCodes(user.getId());
        if (roles.isEmpty()) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号未分配角色");
        }
        String activeRole = resolveActiveRole(roles, client);

        StpUtil.login(user.getId());
        SaSession session = StpUtil.getSession();
        session.set("username", user.getNickname());
        session.set("roles", roles);
        session.set("activeRole", activeRole);

        User update = new User();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        log.info("用户登录成功 userId={} role={} client={}", user.getId(), activeRole, client);
        return WxLoginVO.builder()
                .token(StpUtil.getTokenValue())
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .activeRole(activeRole)
                .roles(roles)
                .expireSeconds(StpUtil.getTokenTimeout())
                .build();
    }

    private String resolveActiveRole(List<String> roles, String client) {
        if ("admin-web".equalsIgnoreCase(client)) {
            if (!roles.contains(RoleConstants.ADMIN)) {
                throw new BizException(ErrorCode.FORBIDDEN, "该账号无管理员权限");
            }
            return RoleConstants.ADMIN;
        }
        if (roles.contains(RoleConstants.ADMIN)) {
            return RoleConstants.ADMIN;
        }
        if (roles.contains(RoleConstants.WORKER)) {
            return RoleConstants.WORKER;
        }
        return roles.get(0);
    }
}
