package com.share.controller;

import jakarta.servlet.http.HttpServletRequest;

final class RequestMetaUtil {

    private RequestMetaUtil() {
    }

    /**
     * 解析客户端 IP：
     * 优先 X-Forwarded-For 首地址，其次 X-Real-IP，最后退回 remoteAddr。
     * 统一截断长度用于安全落库，避免异常超长请求头污染审计数据。
     */
    static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] values = forwarded.split(",");
            if (values.length > 0 && !values[0].isBlank()) {
                return trimMax(values[0].trim(), 64);
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return trimMax(realIp.trim(), 64);
        }

        return trimMax(request.getRemoteAddr(), 64);
    }

    /**
     * 解析并截断 User-Agent，供审计日志记录。
     */
    static String resolveUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return trimMax(request.getHeader("User-Agent"), 255);
    }

    /**
     * 字段截断工具：空值返回 null，超长则按 maxLength 裁剪。
     */
    private static String trimMax(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
