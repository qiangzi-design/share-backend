package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin/dashboard", "/api/admin/dashboard"})
/**
 * 管理端仪表盘入口：
 * - overview 返回核心计数卡片；
 * - trends 返回趋势序列（按天/按月）。
 */
public class AdminDashboardController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(CurrentUserService currentUserService,
                                    AdminAccessService adminAccessService,
                                    AdminDashboardService adminDashboardService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.adminDashboardService = adminDashboardService;
    }

    // 仪表盘总览卡片。
    @GetMapping("/overview")
    public ApiResponse getOverview() {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.DASHBOARD_READ);
        return ApiResponse.success(adminDashboardService.getOverview());
    }

    // 仪表盘趋势图数据。
    @GetMapping("/trends")
    public ApiResponse getTrends(@RequestParam(defaultValue = "day") String granularity,
                                 @RequestParam(required = false) Integer days) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.DASHBOARD_READ);
        return ApiResponse.success(adminDashboardService.getTrends(granularity, days));
    }
}
