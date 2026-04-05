-- Rollback for phase 14 media enhancement

SET @has_contents_videos := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND COLUMN_NAME = 'videos'
);

SET @sql := IF(
    @has_contents_videos > 0,
    "ALTER TABLE contents DROP COLUMN videos",
    "SELECT 1"
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
