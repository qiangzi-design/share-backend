-- Phase 3 rollback (structure only). Business data in follows will be lost after drop.
USE share_db;

-- 1) drop follows table
DROP TABLE IF EXISTS follows;

-- 2) collections index rollback
SET @has_idx_collections_content_id := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'collections' AND index_name = 'idx_collections_content_id'
);
SET @sql := IF(@has_idx_collections_content_id > 0,
               'DROP INDEX idx_collections_content_id ON collections',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_uk_collections_user_content := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'collections' AND index_name = 'uk_collections_user_content'
);
SET @sql := IF(@has_uk_collections_user_content > 0,
               'ALTER TABLE collections DROP INDEX uk_collections_user_content',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) drop contents.collection_count
SET @has_collection_count := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'contents' AND column_name = 'collection_count'
);
SET @sql := IF(@has_collection_count > 0,
               'ALTER TABLE contents DROP COLUMN collection_count',
               'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
