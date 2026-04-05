-- 手工回滚脚本（无 Flyway）
-- 注意：仅回滚结构命名调整，不回滚业务数据
USE share_db;

-- 1) tags.use_count -> tags.count
SET @has_old_count := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tags' AND column_name = 'count'
);
SET @has_use_count := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tags' AND column_name = 'use_count'
);
SET @sql := IF(@has_old_count = 0 AND @has_use_count > 0,
               'ALTER TABLE tags CHANGE COLUMN use_count `count` INT DEFAULT 0',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) tags -> tag
SET @has_tag := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'tag'
);
SET @has_tags := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'tags'
);
SET @sql := IF(@has_tag = 0 AND @has_tags > 0,
               'RENAME TABLE tags TO tag',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) comment_likes -> comment_like
SET @has_comment_like := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'comment_like'
);
SET @has_comment_likes := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'comment_likes'
);
SET @sql := IF(@has_comment_like = 0 AND @has_comment_likes > 0,
               'RENAME TABLE comment_likes TO comment_like',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) users.avatar / users.bio 移除（如需回滚结构）
SET @has_avatar := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'avatar'
);
SET @sql := IF(@has_avatar > 0,
               'ALTER TABLE users DROP COLUMN avatar',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_bio := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'bio'
);
SET @sql := IF(@has_bio > 0,
               'ALTER TABLE users DROP COLUMN bio',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4.1) 回滚 contents.image_size
SET @has_image_size := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'image_size'
);
SET @sql := IF(@has_image_size > 0,
               'ALTER TABLE contents DROP COLUMN image_size',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) 删除搜索索引
SET @has_idx_contents_title := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'contents' AND index_name = 'idx_contents_title'
);
SET @sql := IF(@has_idx_contents_title > 0,
               'DROP INDEX idx_contents_title ON contents',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
