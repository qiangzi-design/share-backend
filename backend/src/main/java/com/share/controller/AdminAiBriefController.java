package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.security.AdminAccessService;
import com.share.security.AdminPermissionCodes;
import com.share.security.CurrentUserService;
import com.share.service.DailyAiBriefService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 管理端AI快讯操作入口：
 * - 提供手动刷新能力，便于临时修复抓取失败或补跑。
 */
@RestController
@RequestMapping({"/admin/ai-brief", "/api/admin/ai-brief"})
public class  AdminAiBriefController {

    private final CurrentUserService currentUserService;
    private final AdminAccessService adminAccessService;
    private final DailyAiBriefService dailyAiBriefService;

    public AdminAiBriefController(CurrentUserService currentUserService,
                                  AdminAccessService adminAccessService,
                                  DailyAiBriefService dailyAiBriefService) {
        this.currentUserService = currentUserService;
        this.adminAccessService = adminAccessService;
        this.dailyAiBriefService = dailyAiBriefService;
    }

    /**
     * 手动刷新AI快讯。
     * 若不传 date，则默认刷新当天。
     */
    @PostMapping("/refresh")
    public ApiResponse refresh(@RequestParam(value = "date", required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               HttpServletRequest request) {
        Long userId = currentUserService.requireCurrentUserId();
        adminAccessService.requirePermission(userId, AdminPermissionCodes.ANALYTICS_READ);
        return ApiResponse.success(dailyAiBriefService.refreshBrief(
                date,
                userId,
                RequestMetaUtil.resolveClientIp(request),
                RequestMetaUtil.resolveUserAgent(request)
        ));
    }
}
