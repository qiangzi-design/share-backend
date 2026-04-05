-- Phase 8: chat unread state and unread statistics indexes

SET @has_is_read = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_messages'
      AND COLUMN_NAME = 'is_read'
);
SET @sql_add_is_read = IF(
    @has_is_read = 0,
    "ALTER TABLE chat_messages ADD COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读'",
    "SELECT 1"
);
PREPARE stmt_add_is_read FROM @sql_add_is_read;
EXECUTE stmt_add_is_read;
DEALLOCATE PREPARE stmt_add_is_read;

SET @has_read_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_messages'
      AND COLUMN_NAME = 'read_time'
);
SET @sql_add_read_time = IF(
    @has_read_time = 0,
    "ALTER TABLE chat_messages ADD COLUMN read_time TIMESTAMP NULL COMMENT '已读时间'",
    "SELECT 1"
);
PREPARE stmt_add_read_time FROM @sql_add_read_time;
EXECUTE stmt_add_read_time;
DEALLOCATE PREPARE stmt_add_read_time;

-- 历史消息初始化为已读，避免上线后未读数异常暴涨
UPDATE chat_messages
SET is_read = 1,
    read_time = COALESCE(read_time, create_time)
WHERE is_read = 0;

SET @has_idx_receiver_read = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_messages'
      AND INDEX_NAME = 'idx_chat_messages_receiver_read'
);
SET @sql_add_idx_receiver_read = IF(
    @has_idx_receiver_read = 0,
    "CREATE INDEX idx_chat_messages_receiver_read ON chat_messages(receiver_id, is_read, conversation_id)",
    "SELECT 1"
);
PREPARE stmt_add_idx_receiver_read FROM @sql_add_idx_receiver_read;
EXECUTE stmt_add_idx_receiver_read;
DEALLOCATE PREPARE stmt_add_idx_receiver_read;

SET @has_idx_conversation_receiver_read = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_messages'
      AND INDEX_NAME = 'idx_chat_messages_conversation_receiver_read'
);
SET @sql_add_idx_conversation_receiver_read = IF(
    @has_idx_conversation_receiver_read = 0,
    "CREATE INDEX idx_chat_messages_conversation_receiver_read ON chat_messages(conversation_id, receiver_id, is_read)",
    "SELECT 1"
);
PREPARE stmt_add_idx_conversation_receiver_read FROM @sql_add_idx_conversation_receiver_read;
EXECUTE stmt_add_idx_conversation_receiver_read;
DEALLOCATE PREPARE stmt_add_idx_conversation_receiver_read;
