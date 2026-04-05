package com.share.controller;

import com.share.dto.ApiResponse;
import com.share.dto.PageResult;
import com.share.security.CurrentUserService;
import com.share.service.AnnouncementService;
import com.share.service.ChatService;
import com.share.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/notifications", "/api/notifications"})
/**
 * 互动通知控制器。
 * 提供通知列表、未读统计、单条已读和全部已读能力。
 */
public class NotificationController {

    private final NotificationService notificationService;
    private final ChatService chatService;
    private final AnnouncementService announcementService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService,
                                  ChatService chatService,
                                  AnnouncementService announcementService,
                                  CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.chatService = chatService;
        this.announcementService = announcementService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/list")
    public ApiResponse getMyNotifications(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "20") Integer pageSize,
                                          @RequestParam(required = false) String category) {
        // 只返回当前登录用户通知，防止越权读取他人消息。
        Long userId = currentUserService.requireCurrentUserId();
        PageResult<Map<String, Object>> result = notificationService.getMyNotifications(userId, page, pageSize, category);
        return ApiResponse.success(result);
    }

    @GetMapping("/unread-count")
    public ApiResponse getUnreadCount() {
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(Map.of("unreadCount", notificationService.getUnreadCount(userId)));
    }

    @GetMapping("/unread-summary")
    public ApiResponse getUnreadSummary() {
        // 一次聚合三类未读，减少前端并行请求次数，降低高并发场景下的接口与数据库压力。
        Long userId = currentUserService.requireCurrentUserId();
        Long interactionUnreadCount = notificationService.getUnreadCountByCategory(userId, "interaction");
        Long systemUnreadCount = notificationService.getUnreadCountByCategory(userId, "system");
        Long notificationUnreadCount = interactionUnreadCount + systemUnreadCount;
        Long chatUnreadCount = chatService.getUnreadCount(userId);
        Long announcementUnreadCount = announcementService.getUnreadAnnouncementCount(userId);
        Long totalUnreadCount = notificationUnreadCount + chatUnreadCount + announcementUnreadCount;
        return ApiResponse.success(Map.of(
                "interactionUnreadCount", interactionUnreadCount,
                "systemUnreadCount", systemUnreadCount,
                "notificationUnreadCount", notificationUnreadCount,
                "chatUnreadCount", chatUnreadCount,
                "announcementUnreadCount", announcementUnreadCount,
                "totalUnreadCount", totalUnreadCount
        ));
    }

    @PostMapping("/read-all")
    public ApiResponse markAllRead() {
        // 一键已读用于快速清空角标。
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(notificationService.markAllRead(userId));
    }

    @PostMapping("/read")
    public ApiResponse markRead(@RequestParam Long id) {
        // 单条已读用于细粒度消息处理，不影响其他通知状态。
        Long userId = currentUserService.requireCurrentUserId();
        return ApiResponse.success(notificationService.markRead(userId, id));
    }
}
