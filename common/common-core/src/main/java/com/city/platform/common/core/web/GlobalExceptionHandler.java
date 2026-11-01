package com.city.platform.common.core.web;

import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理：统一转换为 R，业务异常 HTTP 200。
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("参数校验失败 {}", errors);
        return R.fail(ErrorCode.PARAM_INVALID, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public R<Map<String, String>> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getAllValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            String key = parameter == null ? "param" : parameter;
            result.getResolvableErrors().forEach(error ->
                    errors.putIfAbsent(key, error.getDefaultMessage()));
        });
        log.warn("参数校验失败 {}", errors);
        return R.fail(ErrorCode.PARAM_INVALID, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public R<Map<String, String>> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        log.warn("参数校验失败 {}", errors);
        return R.fail(ErrorCode.PARAM_INVALID, errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数 {}", e.getParameterName());
        return R.fail(ErrorCode.PARAM_INVALID, "缺少参数：" + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体无法解析 {}", e.getMessage());
        return R.fail(ErrorCode.BODY_NOT_READABLE);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("未捕获异常", e);
        return R.fail(ErrorCode.SYSTEM_ERROR);
    }
}
