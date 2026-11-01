package com.city.platform.user.service;

import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;

/**
 * 邮箱验证码：6 位数字、5 分钟有效、校验后即作废。
 * 限速三道闸：同邮箱 60 秒冷却、单邮箱单日 10 条、单 IP 单日 20 条。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final int DAILY_PER_EMAIL = 10;
    private static final int DAILY_PER_IP = 20;

    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;

    /**
     * 邮箱格式粗校验：含 @、@ 后有域名点、长度不超过 128。
     */
    public static boolean validEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        String value = email.trim();
        return value.length() <= 128
                && value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    public void send(String email, String ip) {
        String target = normalize(email);
        if (!validEmail(target)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "邮箱格式不正确");
        }
        String today = LocalDate.now().toString();
        Long emailCount = redisTemplate.opsForValue().increment(dayKey("email", today, target));
        if (emailCount != null && emailCount == 1) {
            redisTemplate.expire(dayKey("email", today, target), Duration.ofDays(1));
        }
        if (emailCount != null && emailCount > DAILY_PER_EMAIL) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "该邮箱今日发送次数已达上限，请明天再试");
        }
        if (StringUtils.hasText(ip)) {
            Long ipCount = redisTemplate.opsForValue().increment(dayKey("ip", today, ip));
            if (ipCount != null && ipCount == 1) {
                redisTemplate.expire(dayKey("ip", today, ip), Duration.ofDays(1));
            }
            if (ipCount != null && ipCount > DAILY_PER_IP) {
                throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "验证码发送太频繁，请稍后再试");
            }
        }
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(cooldownKey(target), "1", COOLDOWN);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "验证码发送太频繁，请 60 秒后再试");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(codeKey(target), code, CODE_TTL);
        mailService.sendCode(target, code);
        log.info("[mail] 邮箱验证码已发送 email={}", target);
    }

    public void verify(String email, String code) {
        String target = normalize(email);
        String saved = redisTemplate.opsForValue().get(codeKey(target));
        if (!StringUtils.hasText(saved) || !saved.equals(code == null ? "" : code.trim())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "验证码错误或已过期");
        }
        redisTemplate.delete(codeKey(target));
    }

    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String codeKey(String email) {
        return "city:email:code:" + email;
    }

    private String cooldownKey(String email) {
        return "city:email:cooldown:" + email;
    }

    private String dayKey(String type, String day, String value) {
        return "city:email:day:" + type + ":" + day + ":" + value;
    }
}
