package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.service.UvStatsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点 UV 上报入口。
 * 说明：
 * - 匿名可访问；
 * - 仅使用前端 viewer_key 作为唯一去重标识。
 */
@RestController
@RequestMapping({"/analytics/uv", "/api/analytics/uv"})
public class UvStatsController {

    private final UvStatsService uvStatsService;

    public UvStatsController(UvStatsService uvStatsService) {
        this.uvStatsService = uvStatsService;
    }

    @PostMapping("/ping")
    public ApiResponse ping(@RequestHeader(value = "X-Viewer-Id", required = false) String viewerId) {
        // UV 统一口径：只认 viewerId。无 viewerId 时忽略本次上报，不抛错。
        uvStatsService.recordVisit(viewerId);
        return ApiResponse.success();
    }
}
