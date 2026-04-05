-- Phase 9: interaction notifications (idempotent + compatibility migration)

-- 1) Ensure table exists.
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者用户ID',
    actor_id BIGINT NOT NULL COMMENT '触发者用户ID',
    content_id BIGINT NULL COMMENT '关联内容ID',
    type VARCHAR(32) NOT NULL COMMENT '通知类型',
    title VARCHAR(120) NOT NULL COMMENT '通知标题',
    body VARCHAR(500) NOT NULL COMMENT '通知内容',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
    read_time TIMESTAMP NULL COMMENT '已读时间',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='互动通知表'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 2) Normalize camelCase columns to snake_case when old schema exists.
SET @has_receiver_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'receiver_id'
);
SET @has_user_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'user_id'
);
SET @has_receiverId = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'receiverId'
);
SET @sql_receiver = IF(
    @has_receiver_id = 0 AND @has_receiverId > 0,
    "ALTER TABLE notifications CHANGE COLUMN receiverId receiver_id BIGINT NOT NULL COMMENT '接收者用户ID'",
    "SELECT 1"
);
PREPARE stmt_receiver FROM @sql_receiver;
EXECUTE stmt_receiver;
DEALLOCATE PREPARE stmt_receiver;

SET @sql_receiver_from_user_id = IF(
    @has_receiver_id = 0 AND @has_user_id > 0,
    "ALTER TABLE notifications CHANGE COLUMN user_id receiver_id BIGINT NOT NULL COMMENT '接收者用户ID'",
    "SELECT 1"
);
PREPARE stmt_receiver_from_user_id FROM @sql_receiver_from_user_id;
EXECUTE stmt_receiver_from_user_id;
DEALLOCATE PREPARE stmt_receiver_from_user_id;

SET @has_actor_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'actor_id'
);
SET @has_actorId = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'actorId'
);
SET @sql_actor = IF(
    @has_actor_id = 0 AND @has_actorId > 0,
    "ALTER TABLE notifications CHANGE COLUMN actorId actor_id BIGINT NOT NULL COMMENT '触发者用户ID'",
    "SELECT 1"
);
PREPARE stmt_actor FROM @sql_actor;
EXECUTE stmt_actor;
DEALLOCATE PREPARE stmt_actor;

SET @has_content_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'content_id'
);
SET @has_contentId = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'contentId'
);
SET @sql_content = IF(
    @has_content_id = 0 AND @has_contentId > 0,
    "ALTER TABLE notifications CHANGE COLUMN contentId content_id BIGINT NULL COMMENT '关联内容ID'",
    "SELECT 1"
);
PREPARE stmt_content FROM @sql_content;
EXECUTE stmt_content;
DEALLOCATE PREPARE stmt_content;

SET @has_is_read = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'is_read'
);
SET @has_isRead = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'isRead'
);
SET @sql_is_read = IF(
    @has_is_read = 0 AND @has_isRead > 0,
    "ALTER TABLE notifications CHANGE COLUMN isRead is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读'",
    "SELECT 1"
);
PREPARE stmt_is_read FROM @sql_is_read;
EXECUTE stmt_is_read;
DEALLOCATE PREPARE stmt_is_read;

SET @has_read_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'read_time'
);
SET @has_readTime = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'readTime'
);
SET @sql_read_time = IF(
    @has_read_time = 0 AND @has_readTime > 0,
    "ALTER TABLE notifications CHANGE COLUMN readTime read_time TIMESTAMP NULL COMMENT '已读时间'",
    "SELECT 1"
);
PREPARE stmt_read_time FROM @sql_read_time;
EXECUTE stmt_read_time;
DEALLOCATE PREPARE stmt_read_time;

SET @has_create_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'create_time'
);
SET @has_createTime = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'createTime'
);
SET @sql_create_time = IF(
    @has_create_time = 0 AND @has_createTime > 0,
    "ALTER TABLE notifications CHANGE COLUMN createTime create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
    "SELECT 1"
);
PREPARE stmt_create_time FROM @sql_create_time;
EXECUTE stmt_create_time;
DEALLOCATE PREPARE stmt_create_time;

-- 3) Add missing required columns (for partially-created old table).
SET @has_receiver_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'receiver_id'
);
SET @sql_add_receiver = IF(
    @has_receiver_id = 0,
    "ALTER TABLE notifications ADD COLUMN receiver_id BIGINT NOT NULL COMMENT '接收者用户ID'",
    "SELECT 1"
);
PREPARE stmt_add_receiver FROM @sql_add_receiver;
EXECUTE stmt_add_receiver;
DEALLOCATE PREPARE stmt_add_receiver;

SET @has_actor_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'actor_id'
);
SET @sql_add_actor = IF(
    @has_actor_id = 0,
    "ALTER TABLE notifications ADD COLUMN actor_id BIGINT NOT NULL COMMENT '触发者用户ID'",
    "SELECT 1"
);
PREPARE stmt_add_actor FROM @sql_add_actor;
EXECUTE stmt_add_actor;
DEALLOCATE PREPARE stmt_add_actor;

SET @has_content_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'content_id'
);
SET @sql_add_content = IF(
    @has_content_id = 0,
    "ALTER TABLE notifications ADD COLUMN content_id BIGINT NULL COMMENT '关联内容ID'",
    "SELECT 1"
);
PREPARE stmt_add_content FROM @sql_add_content;
EXECUTE stmt_add_content;
DEALLOCATE PREPARE stmt_add_content;

SET @has_type = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'type'
);
SET @sql_add_type = IF(
    @has_type = 0,
    "ALTER TABLE notifications ADD COLUMN type VARCHAR(32) NOT NULL COMMENT '通知类型'",
    "SELECT 1"
);
PREPARE stmt_add_type FROM @sql_add_type;
EXECUTE stmt_add_type;
DEALLOCATE PREPARE stmt_add_type;

SET @type_data_type = (
    SELECT DATA_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'type'
    LIMIT 1
);
SET @sql_fix_type_type = IF(
    @type_data_type IS NULL OR @type_data_type IN ('varchar', 'char'),
    "SELECT 1",
    "ALTER TABLE notifications MODIFY COLUMN type VARCHAR(32) NOT NULL COMMENT '通知类型'"
);
PREPARE stmt_fix_type_type FROM @sql_fix_type_type;
EXECUTE stmt_fix_type_type;
DEALLOCATE PREPARE stmt_fix_type_type;

SET @has_title = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'title'
);
SET @sql_add_title = IF(
    @has_title = 0,
    "ALTER TABLE notifications ADD COLUMN title VARCHAR(120) NOT NULL COMMENT '通知标题'",
    "SELECT 1"
);
PREPARE stmt_add_title FROM @sql_add_title;
EXECUTE stmt_add_title;
DEALLOCATE PREPARE stmt_add_title;

SET @has_body = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'body'
);
SET @sql_add_body = IF(
    @has_body = 0,
    "ALTER TABLE notifications ADD COLUMN body VARCHAR(500) NOT NULL COMMENT '通知内容'",
    "SELECT 1"
);
PREPARE stmt_add_body FROM @sql_add_body;
EXECUTE stmt_add_body;
DEALLOCATE PREPARE stmt_add_body;

SET @has_is_read = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'is_read'
);
SET @sql_add_is_read = IF(
    @has_is_read = 0,
    "ALTER TABLE notifications ADD COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读'",
    "SELECT 1"
);
PREPARE stmt_add_is_read FROM @sql_add_is_read;
EXECUTE stmt_add_is_read;
DEALLOCATE PREPARE stmt_add_is_read;

SET @has_read_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'read_time'
);
SET @sql_add_read_time = IF(
    @has_read_time = 0,
    "ALTER TABLE notifications ADD COLUMN read_time TIMESTAMP NULL COMMENT '已读时间'",
    "SELECT 1"
);
PREPARE stmt_add_read_time FROM @sql_add_read_time;
EXECUTE stmt_add_read_time;
DEALLOCATE PREPARE stmt_add_read_time;

SET @has_create_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'create_time'
);
SET @sql_add_create_time = IF(
    @has_create_time = 0,
    "ALTER TABLE notifications ADD COLUMN create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
    "SELECT 1"
);
PREPARE stmt_add_create_time FROM @sql_add_create_time;
EXECUTE stmt_add_create_time;
DEALLOCATE PREPARE stmt_add_create_time;

-- 3.5) Compatibility for legacy extra column user_id:
-- if both receiver_id and user_id exist, keep user_id nullable to avoid insert failures.
SET @has_receiver_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'receiver_id'
);
SET @has_user_id = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND COLUMN_NAME = 'user_id'
);
SET @sql_fix_user_id_nullable = IF(
    @has_receiver_id > 0 AND @has_user_id > 0,
    "ALTER TABLE notifications MODIFY COLUMN user_id BIGINT NULL",
    "SELECT 1"
);
PREPARE stmt_fix_user_id_nullable FROM @sql_fix_user_id_nullable;
EXECUTE stmt_fix_user_id_nullable;
DEALLOCATE PREPARE stmt_fix_user_id_nullable;

-- 4) Ensure indexes exist.
SET @has_idx_receiver_read_time = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND INDEX_NAME = 'idx_notifications_receiver_read_time'
);
SET @sql_add_idx_receiver_read_time = IF(
    @has_idx_receiver_read_time = 0,
    "CREATE INDEX idx_notifications_receiver_read_time ON notifications(receiver_id, is_read, create_time)",
    "SELECT 1"
);
PREPARE stmt_add_idx_receiver_read_time FROM @sql_add_idx_receiver_read_time;
EXECUTE stmt_add_idx_receiver_read_time;
DEALLOCATE PREPARE stmt_add_idx_receiver_read_time;

SET @has_idx_receiver_time = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'notifications'
      AND INDEX_NAME = 'idx_notifications_receiver_time'
);
SET @sql_add_idx_receiver_time = IF(
    @has_idx_receiver_time = 0,
    "CREATE INDEX idx_notifications_receiver_time ON notifications(receiver_id, create_time)",
    "SELECT 1"
);
PREPARE stmt_add_idx_receiver_time FROM @sql_add_idx_receiver_time;
EXECUTE stmt_add_idx_receiver_time;
DEALLOCATE PREPARE stmt_add_idx_receiver_time;
