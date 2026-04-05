package com.share.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 旧版接口路径兼容提示拦截器
 * 旧路径：/api/api/*
 */
@Component
public class LegacyApiPathInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String servletPath = request.getServletPath();
        if (servletPath != null && servletPath.startsWith("/api/")) {
            response.addHeader("Deprecation", "true");
            response.addHeader("Sunset", "Wed, 30 Sep 2026 00:00:00 GMT");
            response.addHeader("Link", "</api>; rel=\"successor-version\"");
        }
        return true;
    }
}

