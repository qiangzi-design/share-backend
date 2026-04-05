package com.share.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求类
 * 用于接收用户注册请求参数
 */
@Data
public class RegisterRequest {
    /**
     * 用户名
     * 必填，长度3-20个字符
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20个字符之间")
    private String username;

    /**
     * 密码
     * 必填，长度至少6个字符
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6个字符")
    private String password;

    /**
     * 邮箱
     * 必填，必须是有效的邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 用户昵称
     * 可选，用户显示名称
     */
    private String nickname;
}