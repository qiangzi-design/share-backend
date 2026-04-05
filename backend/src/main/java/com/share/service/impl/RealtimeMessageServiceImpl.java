package com.share.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.realtime.NotificationWebSocketSessionManager;
import com.share.service.RealtimeMessageService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
/**
 * 服务职责：实时消息推送封装层。
 * 将互动通知/私聊通知统一封装为 WebSocket 事件，降低上层业务对推送协议细节的感知。
 */
public class RealtimeMessageServiceImpl implements RealtimeMessageService {

    private final NotificationWebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public RealtimeMessageServiceImpl(NotificationWebSocketSessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    /**
     * 推送互动类通知（点赞/评论/收藏/公告等）。
     */
    @Override
    public void pushInteractionNotification(Long receiverId, Map<String, Object> payload) {
        push(receiverId, "interaction", payload);
    }

    /**
     * 推送私聊事件通知。
     */
    @Override
    public void pushChatNotification(Long receiverId, Map<String, Object> payload) {
        push(receiverId, "chat", payload);
    }

    /**
     * 推送公告实时事件给在线用户集合。
     * 设计目标：公告属于全局广播事件，采用逐用户发送复用现有单用户通道能力。
     */
    @Override
    public void pushAnnouncementEvent(Collection<Long> receiverIds, Map<String, Object> payload) {
        if (receiverIds == null || receiverIds.isEmpty() || payload == null) {
            return;
        }
        for (Long receiverId : receiverIds) {
            if (receiverId == null || receiverId < 1) {
                continue;
            }
            push(receiverId, "announcement", payload);
        }
    }

    /**
     * 底层统一推送实现。
     * 说明：实时通道失败不影响主业务事务，避免“通知失败导致核心操作回滚”。
     */
    private void push(Long receiverId, String eventType, Map<String, Object> payload) {
        if (receiverId == null || payload == null) {
            return;
        }
        Map<String, Object> message = new HashMap<>();
        message.put("eventType", eventType);
        message.put("payload", payload);
        try {
            sessionManager.sendToUser(receiverId, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ignored) {
            // 序列化失败仅影响实时体验，不中断主流程。
        }
    }
}
