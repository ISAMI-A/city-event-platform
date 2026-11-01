package com.city.platform.common.core.exception;

/**
 * 全局错误码。区间约定见 docs/api.md 第 1.5 节。
 */
public enum ErrorCode {

    SUCCESS(0, "OK"),

    PARAM_INVALID(40001, "参数校验失败"),
    BODY_NOT_READABLE(40002, "请求体格式错误"),

    UNAUTHORIZED(40101, "未登录或登录已失效"),
    BAD_CREDENTIALS(40102, "账号或密码错误"),
    ACCOUNT_DISABLED(40103, "账号已被禁用"),
    WECHAT_LOGIN_FAILED(40104, "微信登录失败，请稍后重试"),
    WECHAT_TICKET_INVALID(40105, "微信绑定凭证已失效，请重新登录"),

    FORBIDDEN(40301, "无权限访问"),
    RESOURCE_NOT_OWNED(40302, "无权操作该资源"),
    INTERNAL_TOKEN_INVALID(40303, "内部调用凭证无效"),

    NOT_FOUND(40401, "资源不存在"),

    STATE_CONFLICT(40901, "当前状态不允许该操作"),
    DUPLICATE_OPERATION(40902, "重复操作或并发冲突"),

    TOO_MANY_REQUESTS(42901, "请求过于频繁"),

    SYSTEM_ERROR(50001, "系统繁忙，请稍后重试"),
    SERVICE_UNAVAILABLE(50301, "服务暂不可用"),

    USER_ALREADY_EXISTS(11001, "用户已存在"),
    TOKEN_USER_MISMATCH(11002, "登录状态与用户不匹配"),
    WECHAT_BIND_CONFLICT(11003, "该微信或账号已绑定其他用户"),

    EVENT_NOT_FOUND(12001, "事件不存在"),
    EVENT_STATE_NOT_ALLOWED(12002, "事件状态不允许该操作"),
    ATTACHMENT_INVALID(12003, "附件数量或类型不合法"),

    WORK_ORDER_NOT_FOUND(13001, "工单不存在"),
    WORK_ORDER_ALREADY_ACCEPTED(13002, "工单已被接单"),
    WORK_ORDER_NOT_OWNED(13003, "工单不属于当前处理人"),

    AI_UNAVAILABLE(14001, "AI 服务暂不可用"),
    AI_OUTPUT_INVALID(14002, "AI 输出校验失败"),

    MAP_UNAVAILABLE(15001, "地图服务异常"),
    GEOCODE_FAILED(15002, "地址解析失败"),

    FILE_TYPE_NOT_SUPPORTED(16001, "文件类型不支持"),
    FILE_TOO_LARGE(16002, "文件过大");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
