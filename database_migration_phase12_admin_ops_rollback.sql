-- Phase 12 rollback: admin operations enhancement

USE share_db;

-- 1) remove role-permission relations for phase-2 permissions
DELETE rp
FROM admin_role_permissions rp
INNER JOIN admin_permissions p ON p.id = rp.permission_id
WHERE p.code IN (
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
);

DELETE FROM admin_permissions
WHERE code IN (
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
);

-- 2) drop indexes
SET @has_idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND INDEX_NAME = 'idx_reports_status_handle_time'
);
SET @sql := IF(@has_idx > 0, "DROP INDEX idx_reports_status_handle_time ON reports", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tags' AND INDEX_NAME = 'idx_tags_status_use'
);
SET @sql := IF(@has_idx > 0, "DROP INDEX idx_tags_status_use ON tags", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'categories' AND INDEX_NAME = 'idx_categories_status_sort'
);
SET @sql := IF(@has_idx > 0, "DROP INDEX idx_categories_status_sort ON categories", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'idx_users_risk_level'
);
SET @sql := IF(@has_idx > 0, "DROP INDEX idx_users_risk_level ON users", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) drop phase-2 tables
DROP TABLE IF EXISTS announcement_reads;
DROP TABLE IF EXISTS announcements;
DROP TABLE IF EXISTS notification_templates;

-- 4) drop reports target snapshot
SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reports' AND COLUMN_NAME = 'target_snapshot'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE reports DROP COLUMN target_snapshot", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) drop users risk fields
SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'risk_level'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE users DROP COLUMN risk_level", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'risk_note'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE users DROP COLUMN risk_note", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'risk_mark_time'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE users DROP COLUMN risk_mark_time", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'risk_mark_by'
);
SET @sql := IF(@has_col > 0, "ALTER TABLE users DROP COLUMN risk_mark_by", "SELECT 1");
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
