package com.city.platform.common.core.web;

import com.city.platform.common.core.constant.ApiConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 从请求头恢复 traceId（网关已生成），缺失时本地生成；响应头回写。
 */
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(ApiConstants.HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = TraceIdUtils.generate();
        }
        TraceIdUtils.setTraceId(traceId);
        response.setHeader(ApiConstants.HEADER_TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceIdUtils.clear();
        }
    }
}
