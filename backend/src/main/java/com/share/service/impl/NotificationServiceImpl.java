package com.share.service.impl;

import com.share.dto.PageResult;
import com.share.entity.Content;
import com.share.entity.Notification;
import com.share.entity.User;
import com.share.mapper.ContentMapper;
import com.share.mapper.NotificationMapper;
import com.share.security.UserStatusCodes;
import com.share.service.NotificationService;
import com.share.service.RealtimeMessageService;
import com.share.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 站内互动通知服务。
 * 负责互动事件入库、未读统计、已读标记，以及实时推送到前端。
 */
public class NotificationServiceImpl implements NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationMapper notificationMapper;
    private final UserService userService;
    private final ContentMapper contentMapper;
    private final RealtimeMessageService realtimeMessageService;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   UserService userService,
                                   ContentMapper contentMapper,
                                   RealtimeMessageService realtimeMessageService) {
        this.notificationMapper = notificationMapper;
        this.userService = userService;
        this.contentMapper = contentMapper;
        this.realtimeMessageService = realtimeMessageService;
    }

    @Override
    @Transactional
    public void createInteractionNotification(Long receiverId,
                                              Long actorId,
                                              Long contentId,
                                              String type,
                                              String extraContent) {
        // 接收人与触发人相同不发通知，避免给自己产生噪音。
        if (receiverId == null || actorId == null || receiverId.equals(actorId)) {
            return;
        }

        // 被封禁账号不再接收互动通知，防止封禁用户继续被打扰。
        User receiver = userService.getById(receiverId);
        if (receiver == null || UserStatusCodes.isBanned(receiver.getStatus())) {
            return;
        }

        User actor = userService.getById(actorId);
        String actorName = actor == null ? "有用户" : getDisplayName(actor);
        Content content = contentId == null ? null : contentMapper.selectById(contentId);
        String contentTitle = content == null || content.getTitle() == null || content.getTitle().isBlank()
                ? "你的内容"
                : "《" + content.getTitle().trim() + "》";

        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setActorId(actorId);
        notification.setContentId(contentId);
        notification.setType(type);
        notification.setTitle(buildTitle(type));
        notification.setBody(buildBody(type, actorName, contentTitle, extraContent));
        notification.setIsRead(Boolean.FALSE);
        notification.setReadTime(null);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);

        pushRealtime(receiverId, notification, actor, content, getUnreadCount(receiverId));
    }

    @Override
    @Transactional
    public void createSystemNotification(Long receiverId, String type, String title, String body) {
        // 系统通知用于治理动作回执（封禁/禁言/下架/隐藏等）。
        if (receiverId == null || receiverId < 1 || title == null || title.isBlank() || body == null || body.isBlank()) {
            return;
        }
        User receiver = userService.getById(receiverId);
        if (receiver == null || UserStatusCodes.isBanned(receiver.getStatus())) {
            return;
        }

        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setActorId(0L);
        notification.setContentId(null);
        notification.setType(type == null || type.isBlank() ? TYPE_SYSTEM_NOTICE : type.trim());
        notification.setTitle(title.trim().length() > 120 ? title.trim().substring(0, 120) : title.trim());
        notification.setBody(body.trim().length() > 500 ? body.trim().substring(0, 500) : body.trim());
        notification.setIsRead(Boolean.FALSE);
        notification.setReadTime(null);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);

        pushRealtime(receiverId, notification, null, null, getUnreadCount(receiverId));
    }

    private void pushRealtime(Long receiverId, Notification notification, User actor, Content content, Long unreadCount) {
        // 统一推送载荷，前端据此同步角标与列表，无需再轮询等待。
        Map<String, Object> payload = new HashMap<>();
        payload.put("notificationId", notification.getId());
        payload.put("type", notification.getType());
        payload.put("title", notification.getTitle());
        payload.put("body", notification.getBody());
        payload.put("actorId", notification.getActorId());
        payload.put("actorUsername", actor == null ? null : actor.getUsername());
        payload.put("actorNickname", actor == null ? null : actor.getNickname());
        payload.put("actorAvatar", actor == null ? null : actor.getAvatar());
        payload.put("contentId", notification.getContentId());
        payload.put("contentTitle", content == null ? null : content.getTitle());
        payload.put("isRead", false);
        payload.put("createTime", notification.getCreateTime());
        payload.put("unreadCount", unreadCount);
        realtimeMessageService.pushInteractionNotification(receiverId, payload);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        Long count = notificationMapper.countUnread(userId, NotificationService.CATEGORY_ALL);
        return count == null ? 0L : count;
    }

    @Override
    public Long getUnreadCountByCategory(Long userId, String category) {
        String normalizedCategory = normalizeCategory(category);
        Long count = notificationMapper.countUnread(userId, normalizedCategory);
        return count == null ? 0L : count;
    }

    @Override
    public PageResult<Map<String, Object>> getMyNotifications(Long userId, Integer page, Integer pageSize) {
        return getMyNotifications(userId, page, pageSize, NotificationService.CATEGORY_ALL);
    }

    @Override
    public PageResult<Map<String, Object>> getMyNotifications(Long userId, Integer page, Integer pageSize, String category) {
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);
        int offset = (validPage - 1) * validPageSize;
        String normalizedCategory = normalizeCategory(category);

        List<Map<String, Object>> list = notificationMapper.findPage(userId, offset, validPageSize, normalizedCategory);
        long total = notificationMapper.countAll(userId, normalizedCategory);

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> markAllRead(Long userId) {
        // 一键已读：批量更新后返回最新未读数，便于前端即时同步角标。
        int updated = notificationMapper.markAllRead(userId);
        Long unreadCount = getUnreadCount(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        result.put("unreadCount", unreadCount);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> markRead(Long userId, Long notificationId) {
        // 单条已读：用于用户按需清理消息，不影响其他通知状态。
        int updated = notificationMapper.markRead(userId, notificationId);
        Long unreadCount = getUnreadCount(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("updated", updated);
        result.put("unreadCount", unreadCount);
        return result;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String buildTitle(String type) {
        return switch (type) {
            case TYPE_CONTENT_LIKE -> "收到新的点赞";
            case TYPE_CONTENT_COMMENT -> "收到新的评论";
            case TYPE_CONTENT_COLLECTION -> "收到新的收藏";
            default -> "收到新的互动";
        };
    }

    private String buildBody(String type, String actorName, String contentTitle, String extraContent) {
        return switch (type) {
            case TYPE_CONTENT_LIKE -> actorName + " 点赞了" + contentTitle;
            case TYPE_CONTENT_COMMENT -> {
                String summary = normalizeSummary(extraContent);
                if (summary.isEmpty()) {
                    yield actorName + " 评论了" + contentTitle;
                }
                yield actorName + " 评论了" + contentTitle + "：" + summary;
            }
            case TYPE_CONTENT_COLLECTION -> actorName + " 收藏了" + contentTitle;
            default -> actorName + " 与你有新的互动";
        };
    }

    private String getDisplayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname().trim();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername().trim();
        }
        return "有用户";
    }

    private String normalizeSummary(String value) {
        if (value == null) {
            return "";
        }
        String text = value.trim().replaceAll("\\s+", " ");
        if (text.length() <= 60) {
            return text;
        }
        return text.substring(0, 60) + "...";
    }

    /**
     * 统一归一化通知分类参数，避免前端传入非法值导致 SQL 口径漂移。
     */
    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return NotificationService.CATEGORY_ALL;
        }
        String normalized = category.trim().toLowerCase();
        if (NotificationService.CATEGORY_SYSTEM.equals(normalized)) {
            return NotificationService.CATEGORY_SYSTEM;
        }
        if (NotificationService.CATEGORY_INTERACTION.equals(normalized)) {
            return NotificationService.CATEGORY_INTERACTION;
        }
        return NotificationService.CATEGORY_ALL;
    }
}
