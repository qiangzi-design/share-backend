package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/admin"})
/**
 * 控制器职责：管理端基础身份入口。
 * 用于前端判断“当前登录用户是否具备管理身份及对应权限集”。
 */
public class AdminController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;

    public AdminController(CurrentUserService currentUserService, AdminAccessService adminAccessService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
    }

    /**
     * 返回当前管理员身份信息（角色 + 权限）。
     * 说明：接口本身不做细粒度权限校验，作为管理端路由守卫初始化依据。
     */
    @GetMapping("/me")
    public ApiResponse getAdminMe() {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(adminAccessService.getAdminMe(userId));
    }
}
