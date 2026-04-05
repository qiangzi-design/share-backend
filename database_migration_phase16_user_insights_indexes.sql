-- Phase 16: user insights performance indexes
-- MySQL idempotent migration script

USE share_db;

SET @has_idx_contents_user_review_status_time := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND INDEX_NAME = 'idx_contents_user_review_status_time'
);
SET @sql := IF(
    @has_idx_contents_user_review_status_time = 0,
    "CREATE INDEX idx_contents_user_review_status_time ON contents(user_id, review_status, status, create_time)",
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
    @has_idx_likes_content_time = 0,
    "CREATE INDEX idx_likes_content_time ON likes(content_id, create_time)",
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
    @has_idx_collections_content_time = 0,
    "CREATE INDEX idx_collections_content_time ON collections(content_id, create_time)",
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
    @has_idx_comments_content_parent_status_review_time = 0,
    "CREATE INDEX idx_comments_content_parent_status_review_time ON comments(content_id, parent_id, status, review_status, create_time)",
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
    @has_idx_reports_target_status_time = 0,
    "CREATE INDEX idx_reports_target_status_time ON reports(target_type, target_id, status, create_time)",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
