package com.share.service;

import com.share.dto.PageResult;

import java.util.Map;

public interface ChatService {

    PageResult<Map<String, Object>> getConversations(Long currentUserId, Integer page, Integer pageSize);

    PageResult<Map<String, Object>> getMessages(Long currentUserId, Long targetUserId, Integer page, Integer pageSize);

    Map<String, Object> sendMessage(Long senderId, Long targetUserId, String content, Integer messageType);

    Map<String, Object> getChatAllowance(Long currentUserId, Long targetUserId);

    Long getUnreadCount(Long currentUserId);

    Map<String, Object> markConversationRead(Long currentUserId, Long targetUserId);
}
