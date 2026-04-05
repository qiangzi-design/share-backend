-- Phase 3 manual migration (MySQL only): collections + follows + stats indexes
-- Safe for existing data. Idempotent and non-destructive.
USE share_db;

-- 1) contents.collection_count
SET @has_collection_count := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'collection_count'
);
SET @sql := IF(@has_collection_count = 0,
               'ALTER TABLE contents ADD COLUMN collection_count INT DEFAULT 0 COMMENT ''收藏次数'' AFTER comment_count',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) follows table
CREATE TABLE IF NOT EXISTS follows (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关注关系ID',
    user_id BIGINT NOT NULL COMMENT '关注者ID',
    target_user_id BIGINT NOT NULL COMMENT '被关注用户ID',
    create_time TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_follows_user_target (user_id, target_user_id),
    KEY idx_follows_target_user_id (target_user_id),
    KEY idx_follows_target_create_time (target_user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3) collections indexes
SET @has_idx_collections_content_id := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'collections' AND index_name = 'idx_collections_content_id'
);
SET @sql := IF(@has_idx_collections_content_id = 0,
               'CREATE INDEX idx_collections_content_id ON collections(content_id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_uk_collections_user_content := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'collections' AND index_name = 'uk_collections_user_content'
);
SET @has_legacy_uk := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'collections' AND index_name = 'user_id'
);
SET @sql := IF(@has_uk_collections_user_content = 0 AND @has_legacy_uk > 0,
               'ALTER TABLE collections RENAME INDEX `user_id` TO uk_collections_user_content',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_uk_collections_user_content := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'collections' AND index_name = 'uk_collections_user_content'
);
SET @sql := IF(@has_uk_collections_user_content = 0,
               'ALTER TABLE collections ADD CONSTRAINT uk_collections_user_content UNIQUE (user_id, content_id)',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) backfill collection_count from existing collections data
UPDATE contents c
LEFT JOIN (
    SELECT content_id, COUNT(*) AS cnt
    FROM collections
    GROUP BY content_id
) cc ON c.id = cc.content_id
SET c.collection_count = COALESCE(cc.cnt, 0);
