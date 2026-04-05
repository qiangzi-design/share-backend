package com.share.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求类
 * 用于接收用户登录请求参数
 */
@Data
public class LoginRequest {
    /**
     * 用户名
     * 必填，不能为空
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     * 必填，不能为空
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}