-- Phase 8 rollback: chat unread state

SET @has_idx_conversation_receiver_read = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_messages'
      AND INDEX_NAME = 'idx_chat_messages_conversation_receiver_read'
);
SET @sql_drop_idx_conversation_receiver_read = IF(
    @has_idx_conversation_receiver_read > 0,
    "DROP INDEX idx_chat_messages_conversation_receiver_read ON chat_messages",
    "SELECT 1"
);
PREPARE stmt_drop_idx_conversation_receiver_read FROM @sql_drop_idx_conversation_receiver_read;
EXECUTE stmt_drop_idx_conversation_receiver_read;
DEALLOCATE PREPARE stmt_drop_idx_conversation_receiver_read;

SET @has_idx_receiver_read = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_messages'
      AND INDEX_NAME = 'idx_chat_messages_receiver_read'
);
SET @sql_drop_idx_receiver_read = IF(
    @has_idx_receiver_read > 0,
    "DROP INDEX idx_chat_messages_receiver_read ON chat_messages",
    "SELECT 1"
);
PREPARE stmt_drop_idx_receiver_read FROM @sql_drop_idx_receiver_read;
EXECUTE stmt_drop_idx_receiver_read;
DEALLOCATE PREPARE stmt_drop_idx_receiver_read;

SET @has_read_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_messages'
      AND COLUMN_NAME = 'read_time'
);
SET @sql_drop_read_time = IF(
    @has_read_time > 0,
    "ALTER TABLE chat_messages DROP COLUMN read_time",
    "SELECT 1"
);
PREPARE stmt_drop_read_time FROM @sql_drop_read_time;
EXECUTE stmt_drop_read_time;
DEALLOCATE PREPARE stmt_drop_read_time;

SET @has_is_read = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'chat_messages'
      AND COLUMN_NAME = 'is_read'
);
SET @sql_drop_is_read = IF(
    @has_is_read > 0,
    "ALTER TABLE chat_messages DROP COLUMN is_read",
    "SELECT 1"
);
PREPARE stmt_drop_is_read FROM @sql_drop_is_read;
EXECUTE stmt_drop_is_read;
DEALLOCATE PREPARE stmt_drop_is_read;
