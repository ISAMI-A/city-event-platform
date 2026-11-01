package com.city.platform.user.service;

import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 邮箱验证码发送：QQ 邮箱 SMTP（SSL 465）。
 * 未配置 MAIL_USER 时按 city.mail.log-code 把验证码打印到日志，便于本地联调。
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String username;
    private final String from;
    private final boolean logCode;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${city.mail.from:}") String from,
                       @Value("${city.mail.log-code:true}") boolean logCode) {
        this.mailSender = mailSender;
        this.username = username;
        this.from = from;
        this.logCode = logCode;
    }

    public void sendCode(String email, String code) {
        if (!StringUtils.hasText(username)) {
            if (logCode) {
                log.warn("[mail] SMTP 未配置，验证码 {} 已输出到日志（email={}）", code, email);
            } else {
                log.warn("[mail] SMTP 未配置且日志兜底已关闭，验证码无法送达 email={}", email);
            }
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(StringUtils.hasText(from) ? from : username);
            helper.setTo(email);
            helper.setSubject("城建平台邮箱验证码");
            helper.setText("您的验证码是：" + code + "，5 分钟内有效。请勿泄露给他人。");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("[mail] 邮件发送失败 email={}: {}", email, e.getMessage());
            throw new BizException(ErrorCode.SERVICE_UNAVAILABLE, "邮件发送失败，请稍后再试");
        }
    }
}
