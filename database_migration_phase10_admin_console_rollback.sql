-- Phase 10 rollback: admin console
-- This rollback removes admin console tables and governance columns.

USE share_db;

-- 1) Drop indexes if exist
SET @has_idx_contents_review_status := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND INDEX_NAME = 'idx_contents_review_status'
);
SET @sql := IF(@has_idx_contents_review_status > 0, "DROP INDEX idx_contents_review_status ON contents", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_comments_review_status := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND INDEX_NAME = 'idx_comments_review_status'
);
SET @sql := IF(@has_idx_comments_review_status > 0, "DROP INDEX idx_comments_review_status ON comments", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Drop governance columns from users
SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'mute_until'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE users DROP COLUMN mute_until", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'ban_reason'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE users DROP COLUMN ban_reason", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'ban_time'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE users DROP COLUMN ban_time", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Drop governance columns from contents
SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contents' AND COLUMN_NAME = 'review_status'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE contents DROP COLUMN review_status", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contents' AND COLUMN_NAME = 'review_reason'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE contents DROP COLUMN review_reason", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contents' AND COLUMN_NAME = 'reviewer_id'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE contents DROP COLUMN reviewer_id", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contents' AND COLUMN_NAME = 'review_time'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE contents DROP COLUMN review_time", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Drop governance columns from comments
SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments' AND COLUMN_NAME = 'review_status'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE comments DROP COLUMN review_status", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments' AND COLUMN_NAME = 'review_reason'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE comments DROP COLUMN review_reason", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments' AND COLUMN_NAME = 'reviewer_id'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE comments DROP COLUMN reviewer_id", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments' AND COLUMN_NAME = 'review_time'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE comments DROP COLUMN review_time", "SELECT 1");
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) Drop admin tables
DROP TABLE IF EXISTS admin_audit_logs;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS admin_user_roles;
DROP TABLE IF EXISTS admin_role_permissions;
DROP TABLE IF EXISTS admin_permissions;
DROP TABLE IF EXISTS admin_roles;
