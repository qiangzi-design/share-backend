-- Phase 16 rollback: remove user insights performance indexes
-- MySQL idempotent rollback script

USE share_db;

SET @has_idx_contents_user_review_status_time := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND INDEX_NAME = 'idx_contents_user_review_status_time'
);
SET @sql := IF(
    @has_idx_contents_user_review_status_time > 0,
    "DROP INDEX idx_contents_user_review_status_time ON contents",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_likes_content_time := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'likes'
      AND INDEX_NAME = 'idx_likes_content_time'
);
SET @sql := IF(
    @has_idx_likes_content_time > 0,
    "DROP INDEX idx_likes_content_time ON likes",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_collections_content_time := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'collections'
      AND INDEX_NAME = 'idx_collections_content_time'
);
SET @sql := IF(
    @has_idx_collections_content_time > 0,
    "DROP INDEX idx_collections_content_time ON collections",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_comments_content_parent_status_review_time := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND INDEX_NAME = 'idx_comments_content_parent_status_review_time'
);
SET @sql := IF(
    @has_idx_comments_content_parent_status_review_time > 0,
    "DROP INDEX idx_comments_content_parent_status_review_time ON comments",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_reports_target_status_time := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reports'
      AND INDEX_NAME = 'idx_reports_target_status_time'
);
SET @sql := IF(
    @has_idx_reports_target_status_time > 0,
    "DROP INDEX idx_reports_target_status_time ON reports",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
