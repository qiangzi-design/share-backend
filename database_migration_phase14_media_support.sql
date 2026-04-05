-- Phase 14: content media enhancement
-- Add videos column to support video publishing

SET @has_contents_videos := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND COLUMN_NAME = 'videos'
);

SET @sql := IF(
    @has_contents_videos = 0,
    "ALTER TABLE contents ADD COLUMN videos VARCHAR(2000) NULL COMMENT 'Video URLs, comma-separated' AFTER images",
    "SELECT 1"
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
