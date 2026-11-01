package com.city.platform.common.security.filter;

import com.city.platform.common.core.constant.ApiConstants;
import com.city.platform.common.security.context.LoginUser;
import com.city.platform.common.security.context.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 用户上下文清理与请求头兜底解析。
 * 正常登录态由 SaTokenWebConfiguration 的拦截器基于共享会话写入；
 * 此处仅解析网关注入的 X-User-*（如启用），并保证请求结束后清理 ThreadLocal。
 */
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Long userId = parseLong(request.getHeader(ApiConstants.HEADER_USER_ID));
        if (userId != null) {
            String username = request.getHeader(ApiConstants.HEADER_USER_NAME);
            String role = request.getHeader(ApiConstants.HEADER_USER_ROLE);
            UserContext.set(new LoginUser(userId, username, role));
            MDC.put("userId", String.valueOf(userId));
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            MDC.remove("userId");
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
