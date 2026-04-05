package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.security.CurrentUserService;
import com.share.service.UserAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/analytics/me", "/api/analytics/me"})
/**
 * 用户侧数据分析控制器。
 * 所有接口仅查询当前登录用户本人数据，不允许跨用户查询。
 */
public class UserAnalyticsController {

    private final CurrentUserService currentUserService;
    private final UserAnalyticsService userAnalyticsService;

    public UserAnalyticsController(CurrentUserService currentUserService,
                                   UserAnalyticsService userAnalyticsService) {
        this.currentUserService = currentUserService;
        this.userAnalyticsService = userAnalyticsService;
    }

    @GetMapping("/overview")
    public ApiResponse getOverview(@RequestParam(defaultValue = "day") String granularity,
                                   @RequestParam(defaultValue = "30") Integer days) {
        // 总览卡片：返回汇总指标与互动率。
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(userAnalyticsService.getOverview(userId, granularity, days));
    }

    @GetMapping("/top-contents")
    public ApiResponse getTopContents(@RequestParam(defaultValue = "5") Integer limit) {
        // Top 作品：默认返回 5 条，可通过 limit 调整上限。
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(userAnalyticsService.getTopContents(userId, limit));
    }

    @GetMapping("/trend")
    public ApiResponse getTrend(@RequestParam(defaultValue = "day") String granularity,
                                @RequestParam(defaultValue = "30") Integer days) {
        // 趋势图：按粒度返回固定时间轴点位。
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(userAnalyticsService.getTrend(userId, granularity, days));
    }

    @GetMapping("/taxonomy")
    public ApiResponse getTaxonomy(@RequestParam(defaultValue = "30") Integer days) {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(userAnalyticsService.getTaxonomy(userId, days));
    }

    @GetMapping("/publish-time")
    public ApiResponse getPublishTime(@RequestParam(defaultValue = "30") Integer days) {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(userAnalyticsService.getPublishTime(userId, days));
    }

    @GetMapping("/governance")
    public ApiResponse getGovernance() {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(userAnalyticsService.getGovernance(userId));
    }
}
