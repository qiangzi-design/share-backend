package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.service.DailyAiBriefService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧每日AI快讯入口：
 * - /today 返回当日快讯（必要时回退最近一日）；
 * - /history 返回近N日摘要，便于历史浏览。
 */
@RestController
@RequestMapping({"/ai-brief", "/api/ai-brief"})
public class AiBriefController {

    private final DailyAiBriefService dailyAiBriefService;

    public AiBriefController(DailyAiBriefService dailyAiBriefService) {
        this.dailyAiBriefService = dailyAiBriefService;
    }

    /** 获取今日AI快讯。 */
    @GetMapping("/today")
    public ApiResponse getTodayBrief() {
        return ApiResponse.success(dailyAiBriefService.getTodayBrief());
    }

    /** 获取历史AI快讯摘要。 */
    @GetMapping("/history")
    public ApiResponse getHistoryBrief(@RequestParam(defaultValue = "7") Integer days) {
        return ApiResponse.success(dailyAiBriefService.getHistoryBriefs(days));
    }
}
