package com.share.dto;

import lombok.Data;

/**
 * API响应封装类
 * 用于统一API返回格式，包含状态码、消息和数据
 */
@Data
public class ApiResponse {
    /**
     * 响应状态码
     * 200: 成功
     * 4xx: 客户端错误
     * 5xx: 服务器错误
     */
    private int code;
    
    /**
     * 响应消息
     * 成功时返回"success"，失败时返回具体错误信息
     */
    private String message;
    
    /**
     * 响应数据
     * 成功时返回业务数据，失败时为null
     */
    private Object data;

    /**
     * 构造方法
     * @param code 状态码
     * @param message 消息
     * @param data 数据
     */
    public ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应（带数据）
     * @param data 响应数据
     * @return ApiResponse对象
     */
    public static ApiResponse success(Object data) {
        return new ApiResponse(200, "success", data);
    }

    /**
     * 创建成功响应（无数据）
     * @return ApiResponse对象
     */
    public static ApiResponse success() {
        return new ApiResponse(200, "success", null);
    }

    /**
     * 创建错误响应
     * @param code 错误状态码
     * @param message 错误消息
     * @return ApiResponse对象
     */
    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message, null);
    }
}