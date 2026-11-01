package com.city.platform.common.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.city.platform.common.security.context.LoginUser;
import com.city.platform.common.security.context.UserContext;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 服务内登录校验与登录上下文构建。
 * 在 Sa-Token 拦截器内部读取共享 Redis 会话，避免在普通 Filter 中提前调用 StpUtil
 * 导致 SaTokenContext 尚未初始化的问题。
 */
@Configuration
public class SaTokenWebConfiguration implements WebMvcConfigurer {

    private static final String[] ANONYMOUS_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/wx-login",
            "/api/v1/auth/wx-bind",
            "/api/v1/auth/wx-register",
            "/api/v1/auth/register",
            "/api/v1/system/ping",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle ->
                        SaRouter.match("/api/v1/**")
                                .notMatch(ANONYMOUS_PATHS)
                                .check(r -> {
                                    StpUtil.checkLogin();
                                    fillUserContext();
                                })))
                .addPathPatterns("/**")
                .excludePathPatterns("/internal/**", "/actuator/**", "/druid/**", "/error");
    }

    private void fillUserContext() {
        Long userId = StpUtil.getLoginIdAsLong();
        SaSession session = StpUtil.getSession(false);
        String username = null;
        String role = null;
        if (session != null) {
            Object sessionUsername = session.get("username");
            Object sessionRole = session.get("activeRole");
            username = sessionUsername == null ? null : String.valueOf(sessionUsername);
            role = sessionRole == null ? null : String.valueOf(sessionRole);
        }
        UserContext.set(new LoginUser(userId, username, role));
        MDC.put("userId", String.valueOf(userId));
    }
}
