package com.city.platform.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.city.platform.gateway.constant.GatewayConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关统一鉴权：白名单放行，其余 /api/v1/** 必须登录。
 */
@Configuration
public class SaTokenGatewayConfiguration {

    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .addExclude("/actuator/**", "/favicon.ico")
                .setAuth(obj -> SaRouter.match("/api/v1/**")
                        .notMatch(GatewayConstants.WHITE_LIST)
                        .check(r -> StpUtil.checkLogin()))
                .setError(e -> {
                    SaHolder.getResponse()
                            .setStatus(401)
                            .setHeader("Content-Type", "application/json;charset=UTF-8");
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("code", GatewayConstants.CODE_UNAUTHORIZED);
                    body.put("message", "未登录或登录已失效");
                    body.put("data", null);
                    return body;
                });
    }
}
