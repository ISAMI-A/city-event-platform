package com.city.platform.common.api.config;

import com.city.platform.common.core.constant.ApiConstants;
import com.city.platform.common.core.web.TraceIdUtils;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 调用头透传：traceId、内部调用凭证、网关注入的用户上下文。
 */
public class FeignRequestInterceptor implements RequestInterceptor {

    private final String internalToken;

    public FeignRequestInterceptor(String internalToken) {
        this.internalToken = internalToken;
    }

    @Override
    public void apply(RequestTemplate template) {
        String traceId = TraceIdUtils.getTraceId();
        if (StringUtils.hasText(traceId)) {
            template.header(ApiConstants.HEADER_TRACE_ID, traceId);
        }
        if (StringUtils.hasText(internalToken)) {
            template.header(ApiConstants.HEADER_INTERNAL_TOKEN, internalToken);
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            copyHeader(attributes, template, ApiConstants.HEADER_USER_ID);
            copyHeader(attributes, template, ApiConstants.HEADER_USER_NAME);
            copyHeader(attributes, template, ApiConstants.HEADER_USER_ROLE);
        }
    }

    private void copyHeader(ServletRequestAttributes attributes, RequestTemplate template, String header) {
        String value = attributes.getRequest().getHeader(header);
        if (StringUtils.hasText(value)) {
            template.header(header, value);
        }
    }
}
