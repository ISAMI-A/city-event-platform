package com.city.platform.common.security.context;

import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;

/**
 * 当前请求的登录用户上下文，由 UserContextFilter 写入并在请求结束后清理。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long requireUserId() {
        return require().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
