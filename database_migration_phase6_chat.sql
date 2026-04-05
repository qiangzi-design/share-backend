-- Phase 6: private chat
-- 支持关注后私聊、单向1条限制、互关不限、聊天记录持久化

CREATE TABLE IF NOT EXISTS chat_conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user_low_id BIGINT NOT NULL COMMENT '较小用户ID',
    user_high_id BIGINT NOT NULL COMMENT '较大用户ID',
    last_message_id BIGINT NULL COMMENT '最后一条消息ID',
    last_message_time TIMESTAMP NULL COMMENT '最后消息时间',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_chat_conversation_pair (user_low_id, user_high_id)
) COMMENT='私聊会话表'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1文本',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间'
) COMMENT='私聊消息表'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_oneway_quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '单向消息配额ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '首条消息使用时间',
    UNIQUE KEY uk_chat_oneway_pair (sender_id, receiver_id)
) COMMENT='单向关注私聊配额表'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_chat_conversation_last_time ON chat_conversations(last_message_time);
CREATE INDEX idx_chat_messages_conversation_id ON chat_messages(conversation_id, id);
CREATE INDEX idx_chat_messages_receiver_time ON chat_messages(receiver_id, create_time);
