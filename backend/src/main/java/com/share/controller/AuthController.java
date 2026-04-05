package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.dto.LoginRequest;
import com.share.dto.LoginResponse;
import com.share.dto.RegisterRequest;
import com.share.entity.User;
import com.share.service.UserService;
import com.share.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * 处理用户注册、登录、登出等认证相关请求
 */
@RestController
@RequestMapping({"/auth", "/api/auth"})
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     * @param request 注册请求参数
     * @return 注册结果
     */
    @PostMapping("/register")
    public ApiResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());

        userService.register(user);
        return ApiResponse.success("注册成功");
    }

    /**
     * 用户登录
     * @param request 登录请求参数
     * @return 登录成功返回JWT令牌和用户信息
     */
    @PostMapping("/login")
    public ApiResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());

        String token = jwtUtil.generateToken(user.getUsername());
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());

        return ApiResponse.success(response);
    }

    /**
     * 用户登出
     * 前端清除本地存储的token即可，后端无需特殊处理
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse logout() {
        return ApiResponse.success("登出成功");
    }
}
