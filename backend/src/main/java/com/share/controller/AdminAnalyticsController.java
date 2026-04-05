package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.AdminAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin/analytics", "/api/admin/analytics"})
/**
 * 管理端运营分析入口：
 * - 统一承接内容质量、用户增长、审核效率三组图表接口。
 * - 每个接口都要求 admin.analytics.read 权限，避免越权读取全站数据。
 */
public class AdminAnalyticsController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(CurrentUserService currentUserService,
                                    AdminAccessService adminAccessService,
                                    AdminAnalyticsService adminAnalyticsService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.adminAnalyticsService = adminAnalyticsService;
    }

    // 内容质量：赞/藏/评/浏览聚合趋势与总览。
    @GetMapping("/content-quality")
    public ApiResponse getContentQuality(@RequestParam(defaultValue = "day") String granularity,
                                         @RequestParam(required = false) Integer days) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.ANALYTICS_READ);
        return ApiResponse.success(adminAnalyticsService.getContentQuality(granularity, days));
    }

    // 用户增长：新增用户与活跃用户趋势。
    @GetMapping("/user-growth")
    public ApiResponse getUserGrowth(@RequestParam(defaultValue = "day") String granularity,
                                     @RequestParam(required = false) Integer days) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.ANALYTICS_READ);
        return ApiResponse.success(adminAnalyticsService.getUserGrowth(granularity, days));
    }

    // 审核效率：工单处理量、解决率与平均时长。
    @GetMapping("/moderation-efficiency")
    public ApiResponse getModerationEfficiency(@RequestParam(defaultValue = "day") String granularity,
                                               @RequestParam(required = false) Integer days) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.ANALYTICS_READ);
        return ApiResponse.success(adminAnalyticsService.getModerationEfficiency(granularity, days));
    }
}
