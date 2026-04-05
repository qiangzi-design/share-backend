package com.share.service;

import com.share.dto.PageResult;

import java.util.Map;

public interface NotificationService {

    String TYPE_CONTENT_LIKE = "content_like";
    String TYPE_CONTENT_COMMENT = "content_comment";
    String TYPE_CONTENT_COLLECTION = "content_collection";
    String TYPE_SYSTEM_NOTICE = "system_notice";
    String CATEGORY_ALL = "all";
    String CATEGORY_INTERACTION = "interaction";
    String CATEGORY_SYSTEM = "system";

    void createInteractionNotification(Long receiverId,
                                       Long actorId,
                                       Long contentId,
                                       String type,
                                       String extraContent);

    void createSystemNotification(Long receiverId,
                                  String type,
                                  String title,
                                  String body);

    Long getUnreadCount(Long userId);

    Long getUnreadCountByCategory(Long userId, String category);

    PageResult<Map<String, Object>> getMyNotifications(Long userId, Integer page, Integer pageSize);

    PageResult<Map<String, Object>> getMyNotifications(Long userId, Integer page, Integer pageSize, String category);

    Map<String, Object> markAllRead(Long userId);

    Map<String, Object> markRead(Long userId, Long notificationId);
}
