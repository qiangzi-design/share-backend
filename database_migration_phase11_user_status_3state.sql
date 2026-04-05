-- Phase 11: user status upgraded to 3 states
-- 1 = normal, 2 = muted, 3 = banned
-- Compatibility: legacy status 0 will be converted to 3 (banned)

UPDATE users
SET status = 3
WHERE status = 0;

UPDATE users
SET status = 1
WHERE status IS NULL;

SET @has_users_mute_until := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'mute_until'
);

SET @sql := IF(
    @has_users_mute_until > 0,
    "UPDATE users SET status = 2 WHERE status = 1 AND mute_until IS NOT NULL AND mute_until > NOW()",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @has_users_mute_until > 0,
    "UPDATE users SET status = 1 WHERE status = 2 AND (mute_until IS NULL OR mute_until <= NOW())",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE users
    MODIFY COLUMN status INT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-禁言，3-封禁';
