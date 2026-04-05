-- Phase 19: daily AI brief module (Python ingestion + Java serve)
-- MySQL idempotent migration script

USE share_db;

CREATE TABLE IF NOT EXISTS daily_ai_briefs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    brief_date DATE NOT NULL COMMENT '快讯日期（按天唯一）',
    title VARCHAR(120) NOT NULL COMMENT '快讯标题',
    summary VARCHAR(800) NULL COMMENT '快讯摘要',
    status VARCHAR(32) NOT NULL DEFAULT 'ready' COMMENT '状态：ready/failed',
    source_count INT NOT NULL DEFAULT 0 COMMENT '采集来源数量',
    item_count INT NOT NULL DEFAULT 0 COMMENT '热点条目数量',
    generated_at DATETIME NOT NULL COMMENT '生成时间',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_daily_ai_briefs_date (brief_date)
) COMMENT='每日AI快讯主表'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS daily_ai_brief_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    brief_id BIGINT NOT NULL COMMENT '所属快讯ID',
    brief_date DATE NOT NULL COMMENT '快讯日期（冗余便于统计）',
    rank_order INT NOT NULL COMMENT '条目排序（1开始）',
    hot_score DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '热度分',
    title VARCHAR(300) NOT NULL COMMENT '热点标题',
    summary VARCHAR(2000) NULL COMMENT '热点摘要',
    source_name VARCHAR(120) NULL COMMENT '来源名称',
    source_url VARCHAR(600) NULL COMMENT '来源链接',
    event_time DATETIME NULL COMMENT '事件时间',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='每日AI快讯条目表'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

SET @has_idx_daily_ai_brief_items_brief_rank := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'daily_ai_brief_items'
      AND INDEX_NAME = 'idx_daily_ai_brief_items_brief_rank'
);
SET @sql := IF(
    @has_idx_daily_ai_brief_items_brief_rank = 0,
    "CREATE INDEX idx_daily_ai_brief_items_brief_rank ON daily_ai_brief_items(brief_id, rank_order)",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_daily_ai_brief_items_date := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'daily_ai_brief_items'
      AND INDEX_NAME = 'idx_daily_ai_brief_items_date'
);
SET @sql := IF(
    @has_idx_daily_ai_brief_items_date = 0,
    "CREATE INDEX idx_daily_ai_brief_items_date ON daily_ai_brief_items(brief_date)",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_daily_ai_brief_items_event_time := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'daily_ai_brief_items'
      AND INDEX_NAME = 'idx_daily_ai_brief_items_event_time'
);
SET @sql := IF(
    @has_idx_daily_ai_brief_items_event_time = 0,
    "CREATE INDEX idx_daily_ai_brief_items_event_time ON daily_ai_brief_items(event_time)",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
