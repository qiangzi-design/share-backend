package com.share.service.impl;

import com.share.dto.PageResult;
import com.share.entity.ChatConversation;
import com.share.entity.ChatMessage;
import com.share.entity.User;
import com.share.exception.BusinessException;
import com.share.mapper.ChatConversationMapper;
import com.share.mapper.ChatMessageMapper;
import com.share.mapper.ChatOneWayQuotaMapper;
import com.share.security.UserStatusCodes;
import com.share.service.ChatService;
import com.share.service.RealtimeMessageService;
import com.share.service.UserFollowService;
import com.share.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
/**
 * 私聊服务。
 * 覆盖会话列表、消息收发、单向关注配额、会话已读与未读统计。
 * 核心规则：单向关注双方各只能发送一条，互关后不限条数。
 */
public class ChatServiceImpl implements ChatService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int TEXT_MESSAGE_TYPE = 1;
    private static final int IMAGE_MESSAGE_TYPE = 2;
    private static final int MAX_TEXT_LENGTH = 1000;

    private final ChatConversationMapper chatConversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatOneWayQuotaMapper chatOneWayQuotaMapper;
    private final UserFollowService userFollowService;
    private final UserService userService;
    private final RealtimeMessageService realtimeMessageService;

    public ChatServiceImpl(ChatConversationMapper chatConversationMapper,
                           ChatMessageMapper chatMessageMapper,
                           ChatOneWayQuotaMapper chatOneWayQuotaMapper,
                           UserFollowService userFollowService,
                           UserService userService,
                           RealtimeMessageService realtimeMessageService) {
        this.chatConversationMapper = chatConversationMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatOneWayQuotaMapper = chatOneWayQuotaMapper;
        this.userFollowService = userFollowService;
        this.userService = userService;
        this.realtimeMessageService = realtimeMessageService;
    }

    @Override
    public PageResult<Map<String, Object>> getConversations(Long currentUserId, Integer page, Integer pageSize) {
        // 会话列表除了消息摘要，还会补充“当前是否可继续发送消息”的业务标记。
        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);
        int offset = (validPage - 1) * validPageSize;

        List<Map<String, Object>> rawList = chatConversationMapper.findConversationSummaries(currentUserId, offset, validPageSize);
        List<Map<String, Object>> list = new ArrayList<>(rawList.size());
        for (Map<String, Object> row : rawList) {
            Long peerUserId = parseLong(row.get("peerUserId"));
            Map<String, Object> item = new HashMap<>(row);
            Long unreadCount = parseLong(item.get("unreadCount"));
            item.put("unreadCount", unreadCount == null ? 0L : unreadCount);
            if (peerUserId != null) {
                item.putAll(buildAllowanceFlags(currentUserId, peerUserId));
            } else {
                item.put("isFollowing", false);
                item.put("isMutual", false);
                item.put("canSend", false);
                item.put("oneWayMessageUsed", false);
            }
            list.add(item);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setList(list);
        result.setTotal(chatConversationMapper.countConversations(currentUserId));
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> getMessages(Long currentUserId, Long targetUserId, Integer page, Integer pageSize) {
        // 历史消息按时间升序返回，方便前端直接渲染时间轴。
        User target = requireActiveTargetUser(targetUserId);

        int validPage = normalizePage(page);
        int validPageSize = normalizePageSize(pageSize);
        int offset = (validPage - 1) * validPageSize;

        ChatConversation conversation = findConversation(currentUserId, target.getId());

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setPage(validPage);
        result.setPageSize(validPageSize);
        if (conversation == null) {
            result.setList(List.of());
            result.setTotal(0L);
            return result;
        }

        List<Map<String, Object>> rows = chatMessageMapper.findMessagePageDesc(conversation.getId(), offset, validPageSize);
        List<Map<String, Object>> ascRows = new ArrayList<>(rows.size());
        for (int i = rows.size() - 1; i >= 0; i--) {
            ascRows.add(rows.get(i));
        }

        result.setList(ascRows);
        result.setTotal(chatMessageMapper.countByConversationId(conversation.getId()));
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> sendMessage(Long senderId, Long targetUserId, String content, Integer messageType) {
        // 先校验目标用户与消息类型，再进入关注关系和配额判定。
        User target = requireActiveTargetUser(targetUserId);
        if (senderId.equals(target.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Cannot send private message to yourself");
        }

        int normalizedMessageType = normalizeMessageType(messageType);
        String cleanContent = normalizeContent(content, normalizedMessageType);

        // 私聊门槛：至少单向关注才能发消息，完全陌生关系直接拒绝。
        boolean isFollowing = userFollowService.isFollowing(senderId, target.getId());
        boolean isFollowedByPeer = userFollowService.isFollowing(target.getId(), senderId);
        boolean hasAnyFollowRelation = isFollowing || isFollowedByPeer;
        if (!hasAnyFollowRelation) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "Follow relation is required before private chat");
        }

        boolean isMutual = isFollowing && isFollowedByPeer;
        if (!isMutual) {
            // 单向关系下通过唯一约束表实现“一人一条”的强约束，避免并发绕过。
            int inserted = chatOneWayQuotaMapper.insertIgnore(senderId, target.getId());
            if (inserted <= 0) {
                throw new BusinessException(HttpStatus.FORBIDDEN, 403, "One-way follow allows only one message per side");
            }
        }

        ChatConversation conversation = ensureConversation(senderId, target.getId());
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(target.getId());
        message.setContent(cleanContent);
        message.setMessageType(normalizedMessageType);
        message.setIsRead(Boolean.FALSE);
        message.setReadTime(null);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        chatConversationMapper.updateLastMessage(conversation.getId(), message.getId(), message.getCreateTime());
        pushChatRealtimeNotification(senderId, target.getId(), conversation.getId(), message);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.getId());
        result.put("messageId", message.getId());
        result.put("senderId", senderId);
        result.put("receiverId", target.getId());
        result.put("content", cleanContent);
        result.put("messageType", normalizedMessageType);
        result.put("createTime", message.getCreateTime());
        result.put("isMutual", isMutual);
        result.put("isFollowing", isFollowing);
        result.put("isFollowedByPeer", isFollowedByPeer);
        result.put("oneWayMessageUsed", !isMutual);
        result.put("canSend", isMutual);
        return result;
    }

    private void pushChatRealtimeNotification(Long senderId, Long receiverId, Long conversationId, ChatMessage message) {
        // 发送成功后实时推送给接收方，前端可立即刷新会话未读角标。
        User sender = userService.getById(senderId);
        Long unreadCount = getUnreadCount(receiverId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("messageId", message.getId());
        payload.put("senderId", senderId);
        payload.put("senderUsername", sender == null ? null : sender.getUsername());
        payload.put("senderNickname", sender == null ? null : sender.getNickname());
        payload.put("senderAvatar", sender == null ? null : sender.getAvatar());
        payload.put("content", message.getContent());
        payload.put("messageType", message.getMessageType());
        payload.put("createTime", message.getCreateTime());
        payload.put("unreadCount", unreadCount);
        realtimeMessageService.pushChatNotification(receiverId, payload);
    }

    @Override
    public Map<String, Object> getChatAllowance(Long currentUserId, Long targetUserId) {
        // 独立暴露“是否可发消息”用于前端输入框态控制，避免用户盲发失败。
        User target = requireActiveTargetUser(targetUserId);
        if (currentUserId.equals(target.getId())) {
            Map<String, Object> self = new HashMap<>();
            self.put("targetUserId", target.getId());
            self.put("isFollowing", false);
            self.put("isFollowedByPeer", false);
            self.put("isMutual", false);
            self.put("canSend", false);
            self.put("oneWayMessageUsed", false);
            self.put("reason", "Cannot send private message to yourself");
            return self;
        }

        Map<String, Object> flags = buildAllowanceFlags(currentUserId, target.getId());
        flags.put("targetUserId", target.getId());
        if (Boolean.TRUE.equals(flags.get("canSend"))) {
            flags.put("reason", "Allowed to send messages");
        } else if (!Boolean.TRUE.equals(flags.get("hasAnyFollowRelation"))) {
            flags.put("reason", "Follow relation is required before private chat");
        } else {
            flags.put("reason", "One-way follow allows one message per side until mutual follow");
        }
        return flags;
    }

    @Override
    public Long getUnreadCount(Long currentUserId) {
        Long unreadCount = chatMessageMapper.countUnreadByReceiverId(currentUserId);
        return unreadCount == null ? 0L : unreadCount;
    }

    @Override
    @Transactional
    public Map<String, Object> markConversationRead(Long currentUserId, Long targetUserId) {
        // 打开会话即已读：仅清理当前会话，不影响其他会话未读。
        User target = requireActiveTargetUser(targetUserId);
        if (currentUserId.equals(target.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Cannot mark self conversation as read");
        }

        int readCount = 0;
        ChatConversation conversation = findConversation(currentUserId, target.getId());
        if (conversation != null) {
            readCount = chatMessageMapper.markConversationRead(conversation.getId(), currentUserId);
        }

        Long unreadCount = getUnreadCount(currentUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("targetUserId", target.getId());
        result.put("readCount", readCount);
        result.put("unreadCount", unreadCount);
        return result;
    }

    private User requireActiveTargetUser(Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Target user is required");
        }
        User target = userService.getById(targetUserId);
        if (target == null || UserStatusCodes.isBanned(target.getStatus())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "Target user not found");
        }
        return target;
    }

    private ChatConversation ensureConversation(Long userA, Long userB) {
        Long low = Math.min(userA, userB);
        Long high = Math.max(userA, userB);
        chatConversationMapper.insertIgnore(low, high);
        ChatConversation conversation = chatConversationMapper.findByPair(low, high);
        if (conversation == null) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, 500, "Failed to create conversation");
        }
        return conversation;
    }

    private ChatConversation findConversation(Long userA, Long userB) {
        Long low = Math.min(userA, userB);
        Long high = Math.max(userA, userB);
        return chatConversationMapper.findByPair(low, high);
    }

    private Map<String, Object> buildAllowanceFlags(Long senderId, Long targetUserId) {
        boolean isFollowing = userFollowService.isFollowing(senderId, targetUserId);
        boolean isFollowedByPeer = userFollowService.isFollowing(targetUserId, senderId);
        boolean hasAnyFollowRelation = isFollowing || isFollowedByPeer;
        boolean isMutual = isFollowing && isFollowedByPeer;
        boolean oneWayMessageUsed = hasAnyFollowRelation && !isMutual && chatOneWayQuotaMapper.countUsed(senderId, targetUserId) > 0;
        boolean canSend = isMutual || (hasAnyFollowRelation && !oneWayMessageUsed);

        Map<String, Object> result = new HashMap<>();
        result.put("isFollowing", isFollowing);
        result.put("isFollowedByPeer", isFollowedByPeer);
        result.put("hasAnyFollowRelation", hasAnyFollowRelation);
        result.put("isMutual", isMutual);
        result.put("canSend", canSend);
        result.put("oneWayMessageUsed", oneWayMessageUsed);
        result.put("oneWayMessageRemaining", isMutual ? -1 : (canSend ? 1 : 0));
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

    private int normalizeMessageType(Integer messageType) {
        if (messageType == null) {
            return TEXT_MESSAGE_TYPE;
        }
        if (messageType != TEXT_MESSAGE_TYPE && messageType != IMAGE_MESSAGE_TYPE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Unsupported message type");
        }
        return messageType;
    }

    private String normalizeContent(String content, int messageType) {
        String cleanContent = content == null ? "" : content.trim();

        if (messageType == IMAGE_MESSAGE_TYPE) {
            if (cleanContent.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Image message content cannot be empty");
            }
            if (!isAllowedChatImageUrl(cleanContent)) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Invalid chat image URL");
            }
            return cleanContent;
        }

        if (cleanContent.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Message content cannot be empty");
        }
        if (cleanContent.length() > MAX_TEXT_LENGTH) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "Message is too long");
        }
        return cleanContent;
    }

    private boolean isAllowedChatImageUrl(String url) {
        return url.startsWith("/api/uploads/chat/")
                || url.startsWith("/uploads/chat/")
                || url.startsWith("uploads/chat/")
                || url.contains("/uploads/chat/");
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
