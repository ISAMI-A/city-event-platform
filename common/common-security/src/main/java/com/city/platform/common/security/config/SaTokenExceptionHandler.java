package com.city.platform.common.security.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常统一转换为错误码，避免落入 50001。
 * 必须高于通用异常处理器，否则会被 Exception 兜底抢先匹配。
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SaTokenExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLogin(NotLoginException e) {
        log.warn("未登录访问：{}", e.getMessage());
        return R.fail(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public R<Void> handleNoPermission(Exception e) {
        log.warn("权限不足：{}", e.getMessage());
        return R.fail(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(SaTokenException.class)
    public R<Void> handleSaToken(SaTokenException e) {
        log.warn("Sa-Token 异常：{}", e.getMessage());
        return R.fail(ErrorCode.UNAUTHORIZED);
    }
}
