-- Phase 10: admin console (RBAC + moderation + reports + audit logs)
-- MySQL idempotent migration script

USE share_db;

-- 1) Governance columns for users
SET @has_users_mute_until := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'mute_until'
);
SET @sql := IF(
    @has_users_mute_until = 0,
    "ALTER TABLE users ADD COLUMN mute_until DATETIME NULL COMMENT 'Muted until time'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_users_ban_reason := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'ban_reason'
);
SET @sql := IF(
    @has_users_ban_reason = 0,
    "ALTER TABLE users ADD COLUMN ban_reason VARCHAR(500) NULL COMMENT 'Ban reason'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_users_ban_time := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'ban_time'
);
SET @sql := IF(
    @has_users_ban_time = 0,
    "ALTER TABLE users ADD COLUMN ban_time DATETIME NULL COMMENT 'Ban time'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Governance columns for contents
SET @has_contents_review_status := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND COLUMN_NAME = 'review_status'
);
SET @sql := IF(
    @has_contents_review_status = 0,
    "ALTER TABLE contents ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'approved' COMMENT 'Review status'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_contents_review_reason := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND COLUMN_NAME = 'review_reason'
);
SET @sql := IF(
    @has_contents_review_reason = 0,
    "ALTER TABLE contents ADD COLUMN review_reason VARCHAR(500) NULL COMMENT 'Review reason'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_contents_reviewer_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND COLUMN_NAME = 'reviewer_id'
);
SET @sql := IF(
    @has_contents_reviewer_id = 0,
    "ALTER TABLE contents ADD COLUMN reviewer_id BIGINT NULL COMMENT 'Reviewer user id'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_contents_review_time := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND COLUMN_NAME = 'review_time'
);
SET @sql := IF(
    @has_contents_review_time = 0,
    "ALTER TABLE contents ADD COLUMN review_time DATETIME NULL COMMENT 'Review time'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE contents
SET review_status = 'approved'
WHERE review_status IS NULL OR review_status = '';

-- 3) Governance columns for comments
SET @has_comments_review_status := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'review_status'
);
SET @sql := IF(
    @has_comments_review_status = 0,
    "ALTER TABLE comments ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT 'approved' COMMENT 'Review status'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_comments_review_reason := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'review_reason'
);
SET @sql := IF(
    @has_comments_review_reason = 0,
    "ALTER TABLE comments ADD COLUMN review_reason VARCHAR(500) NULL COMMENT 'Review reason'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_comments_reviewer_id := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'reviewer_id'
);
SET @sql := IF(
    @has_comments_reviewer_id = 0,
    "ALTER TABLE comments ADD COLUMN reviewer_id BIGINT NULL COMMENT 'Reviewer user id'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_comments_review_time := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'review_time'
);
SET @sql := IF(
    @has_comments_review_time = 0,
    "ALTER TABLE comments ADD COLUMN review_time DATETIME NULL COMMENT 'Review time'",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE comments
SET review_status = 'approved'
WHERE review_status IS NULL OR review_status = '';

-- 4) RBAC tables
CREATE TABLE IF NOT EXISTS admin_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Role id',
    code VARCHAR(64) NOT NULL COMMENT 'Role code',
    name VARCHAR(120) NOT NULL COMMENT 'Role name',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Admin roles'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Permission id',
    code VARCHAR(100) NOT NULL COMMENT 'Permission code',
    name VARCHAR(120) NOT NULL COMMENT 'Permission name',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Admin permissions'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_role_permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Role permission relation id',
    role_id BIGINT NOT NULL COMMENT 'Role id',
    permission_id BIGINT NOT NULL COMMENT 'Permission id',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='Admin role permission relation'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'User role relation id',
    user_id BIGINT NOT NULL COMMENT 'User id',
    role_id BIGINT NOT NULL COMMENT 'Role id',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='Admin user role relation'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 5) Reports + audit logs tables
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Report id',
    reporter_id BIGINT NOT NULL COMMENT 'Reporter user id',
    target_type VARCHAR(32) NOT NULL COMMENT 'Target type: content/comment/user',
    target_id BIGINT NOT NULL COMMENT 'Target id',
    reason VARCHAR(500) NOT NULL COMMENT 'Report reason',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/assigned/resolved/rejected',
    assignee_id BIGINT NULL COMMENT 'Assignee user id',
    handle_note VARCHAR(500) NULL COMMENT 'Handle note',
    resolve_action VARCHAR(64) NULL COMMENT 'Resolve action',
    handle_time DATETIME NULL COMMENT 'Handle time',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Report tickets'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Audit log id',
    operator_user_id BIGINT NOT NULL COMMENT 'Operator user id',
    action VARCHAR(100) NOT NULL COMMENT 'Action code',
    target_type VARCHAR(64) NOT NULL COMMENT 'Target type',
    target_id BIGINT NULL COMMENT 'Target id',
    detail_before JSON NULL COMMENT 'Before snapshot',
    detail_after JSON NULL COMMENT 'After snapshot',
    ip VARCHAR(64) NULL COMMENT 'Client IP',
    user_agent VARCHAR(255) NULL COMMENT 'User agent',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='Admin audit logs'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 6) Indexes (idempotent)
SET @has_idx_admin_roles_code := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_roles'
      AND INDEX_NAME = 'uk_admin_roles_code'
);
SET @sql := IF(
    @has_idx_admin_roles_code = 0,
    "CREATE UNIQUE INDEX uk_admin_roles_code ON admin_roles(code)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_admin_permissions_code := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_permissions'
      AND INDEX_NAME = 'uk_admin_permissions_code'
);
SET @sql := IF(
    @has_idx_admin_permissions_code = 0,
    "CREATE UNIQUE INDEX uk_admin_permissions_code ON admin_permissions(code)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_role_perm_unique := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_role_permissions'
      AND INDEX_NAME = 'uk_admin_role_permission'
);
SET @sql := IF(
    @has_idx_role_perm_unique = 0,
    "CREATE UNIQUE INDEX uk_admin_role_permission ON admin_role_permissions(role_id, permission_id)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_user_role_unique := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_user_roles'
      AND INDEX_NAME = 'uk_admin_user_role'
);
SET @sql := IF(
    @has_idx_user_role_unique = 0,
    "CREATE UNIQUE INDEX uk_admin_user_role ON admin_user_roles(user_id, role_id)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_reports_status_time := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reports'
      AND INDEX_NAME = 'idx_reports_status_time'
);
SET @sql := IF(
    @has_idx_reports_status_time = 0,
    "CREATE INDEX idx_reports_status_time ON reports(status, create_time)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_reports_target := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'reports'
      AND INDEX_NAME = 'idx_reports_target'
);
SET @sql := IF(
    @has_idx_reports_target = 0,
    "CREATE INDEX idx_reports_target ON reports(target_type, target_id)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_audit_operator_time := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_audit_logs'
      AND INDEX_NAME = 'idx_admin_audit_operator_time'
);
SET @sql := IF(
    @has_idx_audit_operator_time = 0,
    "CREATE INDEX idx_admin_audit_operator_time ON admin_audit_logs(operator_user_id, create_time)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_audit_target := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'admin_audit_logs'
      AND INDEX_NAME = 'idx_admin_audit_target'
);
SET @sql := IF(
    @has_idx_audit_target = 0,
    "CREATE INDEX idx_admin_audit_target ON admin_audit_logs(target_type, target_id, create_time)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_contents_review_status := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'contents'
      AND INDEX_NAME = 'idx_contents_review_status'
);
SET @sql := IF(
    @has_idx_contents_review_status = 0,
    "CREATE INDEX idx_contents_review_status ON contents(review_status, status, create_time)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_comments_review_status := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND INDEX_NAME = 'idx_comments_review_status'
);
SET @sql := IF(
    @has_idx_comments_review_status = 0,
    "CREATE INDEX idx_comments_review_status ON comments(review_status, status, create_time)",
    "SELECT 1"
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7) Seed roles
INSERT INTO admin_roles(code, name)
SELECT 'SUPER_ADMIN', 'Super Admin'
WHERE NOT EXISTS (SELECT 1 FROM admin_roles WHERE code = 'SUPER_ADMIN');

INSERT INTO admin_roles(code, name)
SELECT 'CONTENT_MODERATOR', 'Content Moderator'
WHERE NOT EXISTS (SELECT 1 FROM admin_roles WHERE code = 'CONTENT_MODERATOR');

INSERT INTO admin_roles(code, name)
SELECT 'USER_OPS', 'User Operations'
WHERE NOT EXISTS (SELECT 1 FROM admin_roles WHERE code = 'USER_OPS');

INSERT INTO admin_roles(code, name)
SELECT 'AUDITOR_READONLY', 'Auditor Readonly'
WHERE NOT EXISTS (SELECT 1 FROM admin_roles WHERE code = 'AUDITOR_READONLY');

-- 8) Seed permissions
INSERT INTO admin_permissions(code, name)
SELECT 'admin.dashboard.read', 'Dashboard Read'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.dashboard.read');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.user.read', 'User Read'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.user.read');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.user.ban', 'User Ban'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.user.ban');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.user.mute', 'User Mute'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.user.mute');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.content.read', 'Content Read'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.content.read');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.content.off_shelf', 'Content Off Shelf'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.content.off_shelf');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.content.restore', 'Content Restore'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.content.restore');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.comment.read', 'Comment Read'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.comment.read');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.comment.hide', 'Comment Hide'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.comment.hide');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.report.read', 'Report Read'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.report.read');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.report.handle', 'Report Handle'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.report.handle');

INSERT INTO admin_permissions(code, name)
SELECT 'admin.audit.read', 'Audit Read'
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'admin.audit.read');

-- 9) Bind SUPER_ADMIN role with all permissions
INSERT INTO admin_role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
JOIN admin_permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM admin_role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- 10) Bind other roles with scoped permissions
-- CONTENT_MODERATOR
INSERT INTO admin_role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
JOIN admin_permissions p ON p.code IN (
    'admin.dashboard.read',
    'admin.content.read',
    'admin.content.off_shelf',
    'admin.content.restore',
    'admin.comment.read',
    'admin.comment.hide',
    'admin.report.read',
    'admin.report.handle'
)
WHERE r.code = 'CONTENT_MODERATOR'
  AND NOT EXISTS (
      SELECT 1
      FROM admin_role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- USER_OPS
INSERT INTO admin_role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
JOIN admin_permissions p ON p.code IN (
    'admin.dashboard.read',
    'admin.user.read',
    'admin.user.ban',
    'admin.user.mute',
    'admin.report.read',
    'admin.report.handle'
)
WHERE r.code = 'USER_OPS'
  AND NOT EXISTS (
      SELECT 1
      FROM admin_role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- AUDITOR_READONLY
INSERT INTO admin_role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
JOIN admin_permissions p ON p.code IN (
    'admin.dashboard.read',
    'admin.user.read',
    'admin.content.read',
    'admin.comment.read',
    'admin.report.read',
    'admin.audit.read'
)
WHERE r.code = 'AUDITOR_READONLY'
  AND NOT EXISTS (
      SELECT 1
      FROM admin_role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- 11) Bootstrap existing admin user as SUPER_ADMIN
INSERT INTO admin_user_roles(user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN admin_roles r ON r.code = 'SUPER_ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM admin_user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );
