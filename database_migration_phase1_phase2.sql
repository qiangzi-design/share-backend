-- 手工迁移脚本（无 Flyway）
-- 执行前请先备份数据库
USE share_db;

-- 1) 兼容旧表名：comment_like -> comment_likes
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
SET @sql := IF(@has_comment_like > 0 AND @has_comment_likes = 0,
               'RENAME TABLE comment_like TO comment_likes',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 兼容旧表名：tag -> tags
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
SET @sql := IF(@has_tag > 0 AND @has_tags = 0,
               'RENAME TABLE tag TO tags',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 兼容旧字段：tags.count -> tags.use_count
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
SET @sql := IF(@has_old_count > 0 AND @has_use_count = 0,
               'ALTER TABLE tags CHANGE COLUMN `count` use_count INT DEFAULT 0',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) 补充 users 资料字段
SET @has_avatar := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'avatar'
);
SET @sql := IF(@has_avatar = 0,
               'ALTER TABLE users ADD COLUMN avatar VARCHAR(255) NULL COMMENT ''头像URL''',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_bio := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'bio'
);
SET @sql := IF(@has_bio = 0,
               'ALTER TABLE users ADD COLUMN bio TEXT NULL COMMENT ''个人简介''',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4.1) contents 增加图片总大小字段
SET @has_image_size := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'image_size'
);
SET @sql := IF(@has_image_size = 0,
               'ALTER TABLE contents ADD COLUMN image_size BIGINT DEFAULT 0 COMMENT ''图片总大小（字节）'' AFTER images',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) 补充搜索索引
SET @has_idx_contents_title := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'contents' AND index_name = 'idx_contents_title'
);
SET @sql := IF(@has_idx_contents_title = 0,
               'CREATE INDEX idx_contents_title ON contents(title)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
