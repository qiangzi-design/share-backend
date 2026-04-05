package com.share.dto;

import lombok.Data;

/**
 * 登录响应类
 * 用于返回登录成功后的用户信息和令牌
 */
@Data
public class LoginResponse {
    /**
     * JWT令牌
     * 用户认证凭证，用于后续请求的身份验证
     */
    private String token;
    
    /**
     * 用户名
     * 用户登录账号
     */
    private String username;
    
    /**
     * 用户昵称
     * 用户显示名称
     */
    private String nickname;
}