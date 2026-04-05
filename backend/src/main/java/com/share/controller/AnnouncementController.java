package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.security.CurrentUserService;
import com.share.service.AnnouncementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/announcements", "/api/announcements"})
/**
 * 用户侧公告入口：
 * - 首页读取有效公告；
 * - 消息中心读取公告列表与未读统计；
 * - 支持单条/全部已读。
 */
public class AnnouncementController {

    private final CurrentUserService currentUserService;
    private final AnnouncementService announcementService;

    public AnnouncementController(CurrentUserService currentUserService,
                                  AnnouncementService announcementService) {
        this.currentUserService = currentUserService;
        this.announcementService = announcementService;
    }

    // 首页公告：允许匿名访问，已登录用户会附带 isRead 状态。
    @GetMapping("/active")
    public ApiResponse getActiveAnnouncements() {
        Long userId = currentUserService.getCurrentUserIdOrNull();
        return ApiResponse.success(announcementService.getActiveAnnouncements(userId));
    }

    // 公告消息列表（登录用户）。
    @GetMapping("/list")
    public ApiResponse getMyAnnouncements(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(announcementService.getMyAnnouncements(userId, page, pageSize));
    }

    // 公告未读角标。
    @GetMapping("/unread-count")
    public ApiResponse getUnreadCount() {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(Map.of("unreadCount", announcementService.getUnreadAnnouncementCount(userId)));
    }

    @PostMapping("/read")
    public ApiResponse markRead(@RequestParam("id") Long announcementId) {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(announcementService.markAnnouncementRead(userId, announcementId));
    }

    @PostMapping("/read-all")
    public ApiResponse markAllRead() {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(announcementService.markAllAnnouncementsRead(userId));
    }
}
