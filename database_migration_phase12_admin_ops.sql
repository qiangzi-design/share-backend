-- Phase 12: admin operations enhancement (taxonomy + announcements + templates + analytics base)
-- MySQL idempotent migration script

USE share_db;

-- 1) users: risk mark fields
SET @has_users_risk_level := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'risk_level'
);
SET @sql := IF(
    @has_users_risk_level = 0,
    "ALTER TABLE users ADD COLUMN risk_level VARCHAR(16) NULL COMMENT 'Risk level: low/medium/high'",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_users_risk_note := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'risk_note'
);
SET @sql := IF(
    @has_users_risk_note = 0,
    "ALTER TABLE users ADD COLUMN risk_note VARCHAR(500) NULL COMMENT 'Risk note'",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_users_risk_mark_time := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'risk_mark_time'
);
SET @sql := IF(
    @has_users_risk_mark_time = 0,
    "ALTER TABLE users ADD COLUMN risk_mark_time DATETIME NULL COMMENT 'Risk mark time'",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_users_risk_mark_by := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'risk_mark_by'
);
SET @sql := IF(
    @has_users_risk_mark_by = 0,
    "ALTER TABLE users ADD COLUMN risk_mark_by BIGINT NULL COMMENT 'Risk marker user id'",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) reports: target snapshot for closed-loop preview
SET @has_reports_target_snapshot := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND COLUMN_NAME = 'target_snapshot'
);
SET @sql := IF(
    @has_reports_target_snapshot = 0,
    "ALTER TABLE reports ADD COLUMN target_snapshot JSON NULL COMMENT 'Target snapshot json'",
    "SELECT 1"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) announcements
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Announcement id',
    title VARCHAR(120) NOT NULL COMMENT 'Title',
    body VARCHAR(2000) NOT NULL COMMENT 'Body',
    status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT 'draft/published/offline',
    is_pinned TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Pinned',
    start_time DATETIME NULL COMMENT 'Visible start time',
    end_time DATETIME NULL COMMENT 'Visible end time',
    publish_time DATETIME NULL COMMENT 'Publish time',
    creator_id BIGINT NULL COMMENT 'Creator user id',
    updater_id BIGINT NULL COMMENT 'Updater user id',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Site announcements'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS announcement_reads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Read record id',
    announcement_id BIGINT NOT NULL COMMENT 'Announcement id',
    user_id BIGINT NOT NULL COMMENT 'User id',
    read_time DATETIME NOT NULL COMMENT 'Read time',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='Announcement read records'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 4) system notification templates
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Template id',
    code VARCHAR(64) NOT NULL COMMENT 'Template code',
    name VARCHAR(120) NOT NULL COMMENT 'Template name',
    title_template VARCHAR(120) NOT NULL COMMENT 'Title template',
    body_template VARCHAR(800) NOT NULL COMMENT 'Body template',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 enabled 0 disabled',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='System notification templates'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 5) indexes
SET @has_idx_ann_status_time := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcements' AND INDEX_NAME = 'idx_ann_status_time'
);
SET @sql := IF(@has_idx_ann_status_time = 0,
    "CREATE INDEX idx_ann_status_time ON announcements(status, is_pinned, publish_time, start_time, end_time)",
    "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_ann_reads_unique := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcement_reads' AND INDEX_NAME = 'uk_ann_reads_ann_user'
);
SET @sql := IF(@has_idx_ann_reads_unique = 0,
    "CREATE UNIQUE INDEX uk_ann_reads_ann_user ON announcement_reads(announcement_id, user_id)",
    "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_ann_reads_user_time := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'announcement_reads' AND INDEX_NAME = 'idx_ann_reads_user_time'
);
SET @sql := IF(@has_idx_ann_reads_user_time = 0,
    "CREATE INDEX idx_ann_reads_user_time ON announcement_reads(user_id, read_time)",
    "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_tpl_code := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'notification_templates' AND INDEX_NAME = 'uk_notification_templates_code'
);
SET @sql := IF(@has_idx_tpl_code = 0,
    "CREATE UNIQUE INDEX uk_notification_templates_code ON notification_templates(code)",
    "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_users_risk := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'idx_users_risk_level'
);
SET @sql := IF(@has_idx_users_risk = 0,
    "CREATE INDEX idx_users_risk_level ON users(risk_level, status, update_time)",
    "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_categories_status_sort := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND INDEX_NAME = 'idx_categories_status_sort'
);
SET @sql := IF(@has_idx_categories_status_sort = 0,
    "CREATE INDEX idx_categories_status_sort ON categories(status, sort_order)",
    "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_tags_status_use := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tags' AND INDEX_NAME = 'idx_tags_status_use'
);
SET @sql := IF(@has_idx_tags_status_use = 0,
    "CREATE INDEX idx_tags_status_use ON tags(status, use_count)",
    "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_reports_status_time_v2 := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND INDEX_NAME = 'idx_reports_status_handle_time'
);
SET @sql := IF(@has_idx_reports_status_time_v2 = 0,
    "CREATE INDEX idx_reports_status_handle_time ON reports(status, create_time, handle_time)",
    "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6) seed templates
INSERT INTO notification_templates(code, name, title_template, body_template, status)
VALUES
('USER_BANNED', 'User banned notice', '账号封禁通知', '你的账号已被管理员封禁。原因：{{reason}}', 1),
('USER_MUTED', 'User muted notice', '账号禁言通知', '你的账号已被禁言 {{minutes}} 分钟。原因：{{reason}}', 1),
('CONTENT_OFF_SHELF', 'Content off shelf notice', '内容下架通知', '你的内容《{{contentTitle}}》已下架。原因：{{reason}}', 1),
('COMMENT_HIDDEN', 'Comment hidden notice', '评论隐藏通知', '你在《{{contentTitle}}》下的评论已被隐藏。原因：{{reason}}', 1)
ON DUPLICATE KEY UPDATE
name = VALUES(name),
title_template = VALUES(title_template),
body_template = VALUES(body_template),
status = VALUES(status),
update_time = CURRENT_TIMESTAMP;

-- 7) phase-2 permissions
INSERT INTO admin_permissions(code, name)
VALUES
('admin.category.read', 'Admin category read'),
('admin.category.write', 'Admin category write'),
('admin.tag.read', 'Admin tag read'),
('admin.tag.write', 'Admin tag write'),
('admin.announcement.read', 'Admin announcement read'),
('admin.announcement.write', 'Admin announcement write'),
('admin.template.read', 'Admin template read'),
('admin.template.write', 'Admin template write'),
('admin.analytics.read', 'Admin analytics read'),
('admin.user.risk_mark', 'Admin user risk mark')
ON DUPLICATE KEY UPDATE
name = VALUES(name),
update_time = CURRENT_TIMESTAMP;

INSERT INTO admin_role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
INNER JOIN admin_permissions p ON p.code IN (
    'admin.category.read',
    'admin.category.write',
    'admin.tag.read',
    'admin.tag.write',
    'admin.announcement.read',
    'admin.announcement.write',
    'admin.template.read',
    'admin.template.write',
    'admin.analytics.read',
    'admin.user.risk_mark'
)
WHERE r.code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE
    role_id = admin_role_permissions.role_id;
