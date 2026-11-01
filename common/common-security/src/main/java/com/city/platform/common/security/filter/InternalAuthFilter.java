package com.city.platform.common.security.filter;

import com.city.platform.common.core.constant.ApiConstants;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.common.core.model.R;
import com.city.platform.common.security.config.InternalTokenProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 内部接口鉴权：仅允许携带正确 X-Internal-Token 的服务间调用。
 * 未配置密钥时默认拒绝全部内部请求（安全默认值）。
 */
public class InternalAuthFilter extends OncePerRequestFilter {

    private final InternalTokenProperties properties;
    private final ObjectMapper objectMapper;

    public InternalAuthFilter(InternalTokenProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String expected = properties.getInternalToken();
        String actual = request.getHeader(ApiConstants.HEADER_INTERNAL_TOKEN);
        if (!StringUtils.hasText(expected) || !expected.equals(actual)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    R.fail(ErrorCode.INTERNAL_TOKEN_INVALID)));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
