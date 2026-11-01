package com.city.platform.common.api.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Feign 公共配置：统一请求头透传。
 */
@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "feignRequestInterceptor")
    public RequestInterceptor feignRequestInterceptor(
            @Value("${city.security.internal-token:}") String internalToken) {
        return new FeignRequestInterceptor(internalToken);
    }
}
