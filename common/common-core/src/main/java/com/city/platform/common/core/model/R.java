package com.city.platform.common.core.model;

import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.web.TraceIdUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int code;
    private String message;
    private T data;
    private String traceId;
    private String timestamp;

    public static <T> R<T> ok(T data) {
        return build(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static R<Void> ok() {
        return build(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        return build(errorCode.getCode(), message, null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, T data) {
        return build(errorCode.getCode(), errorCode.getMessage(), data);
    }

    public static <T> R<T> fail(int code, String message) {
        return build(code, message, null);
    }

    public boolean isSuccess() {
        return code == ErrorCode.SUCCESS.getCode();
    }

    private static <T> R<T> build(int code, String message, T data) {
        return new R<>(code, message, data, TraceIdUtils.getTraceId(),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER));
    }
}
