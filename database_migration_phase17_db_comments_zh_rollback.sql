-- Phase 17 rollback: revert Chinese DB comments back to English wording
-- MySQL idempotent rollback script

USE share_db;

DROP PROCEDURE IF EXISTS sp_set_column_comment_if_exists;
DROP PROCEDURE IF EXISTS sp_set_table_comment_if_exists;

DELIMITER $$

CREATE PROCEDURE sp_set_column_comment_if_exists(
    IN p_table_name VARCHAR(128),
    IN p_column_name VARCHAR(128),
    IN p_comment VARCHAR(500)
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_column_type TEXT;
    DECLARE v_data_type VARCHAR(64);
    DECLARE v_is_nullable VARCHAR(3);
    DECLARE v_default TEXT;
    DECLARE v_extra TEXT;
    DECLARE v_charset VARCHAR(64);
    DECLARE v_collation VARCHAR(64);
    DECLARE v_default_sql TEXT DEFAULT '';
    DECLARE v_sql LONGTEXT;

    SELECT COUNT(*)
    INTO v_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;

    IF v_exists = 1 THEN
        SELECT COLUMN_TYPE,
               DATA_TYPE,
               IS_NULLABLE,
               COLUMN_DEFAULT,
               EXTRA,
               CHARACTER_SET_NAME,
               COLLATION_NAME
        INTO v_column_type,
             v_data_type,
             v_is_nullable,
             v_default,
             v_extra,
             v_charset,
             v_collation
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
        LIMIT 1;

        IF v_default IS NOT NULL THEN
            IF UPPER(v_default) IN ('CURRENT_TIMESTAMP', 'CURRENT_TIMESTAMP()', 'NOW()', 'LOCALTIME', 'LOCALTIMESTAMP')
               OR UPPER(v_default) LIKE 'CURRENT_TIMESTAMP(%' THEN
                SET v_default_sql = CONCAT(' DEFAULT ', v_default);
            ELSE
                SET v_default_sql = CONCAT(' DEFAULT ', QUOTE(v_default));
            END IF;
        END IF;

        SET v_sql = CONCAT(
            'ALTER TABLE `', p_table_name, '` MODIFY COLUMN `', p_column_name, '` ', v_column_type,
            CASE
                WHEN v_charset IS NOT NULL
                     AND v_data_type IN ('char', 'varchar', 'tinytext', 'text', 'mediumtext', 'longtext', 'enum', 'set')
                THEN CONCAT(' CHARACTER SET ', v_charset, ' COLLATE ', v_collation)
                ELSE ''
            END,
            CASE WHEN v_is_nullable = 'NO' THEN ' NOT NULL' ELSE ' NULL' END,
            v_default_sql,
            CASE WHEN v_extra IS NOT NULL AND v_extra <> '' THEN CONCAT(' ', v_extra) ELSE '' END,
            ' COMMENT ', QUOTE(p_comment)
        );

        SET @ddl_sql = v_sql;
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE sp_set_table_comment_if_exists(
    IN p_table_name VARCHAR(128),
    IN p_comment VARCHAR(500)
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    SELECT COUNT(*)
    INTO v_exists
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name;

    IF v_exists = 1 THEN
        SET @ddl_sql = CONCAT('ALTER TABLE `', p_table_name, '` COMMENT = ', QUOTE(p_comment));
        PREPARE stmt FROM @ddl_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- users / contents / comments
CALL sp_set_column_comment_if_exists('users', 'mute_until', 'Muted until time');
CALL sp_set_column_comment_if_exists('users', 'ban_reason', 'Ban reason');
CALL sp_set_column_comment_if_exists('users', 'ban_time', 'Ban time');
CALL sp_set_column_comment_if_exists('users', 'risk_level', 'Risk level: low/medium/high');
CALL sp_set_column_comment_if_exists('users', 'risk_note', 'Risk note');
CALL sp_set_column_comment_if_exists('users', 'risk_mark_time', 'Risk mark time');
CALL sp_set_column_comment_if_exists('users', 'risk_mark_by', 'Risk marker user id');

CALL sp_set_column_comment_if_exists('contents', 'review_status', 'Review status');
CALL sp_set_column_comment_if_exists('contents', 'review_reason', 'Review reason');
CALL sp_set_column_comment_if_exists('contents', 'reviewer_id', 'Reviewer user id');
CALL sp_set_column_comment_if_exists('contents', 'review_time', 'Review time');
CALL sp_set_column_comment_if_exists('contents', 'videos', 'Video URLs, comma-separated');

CALL sp_set_column_comment_if_exists('comments', 'review_status', 'Review status');
CALL sp_set_column_comment_if_exists('comments', 'review_reason', 'Review reason');
CALL sp_set_column_comment_if_exists('comments', 'reviewer_id', 'Reviewer user id');
CALL sp_set_column_comment_if_exists('comments', 'review_time', 'Review time');

-- RBAC
CALL sp_set_column_comment_if_exists('admin_roles', 'id', 'Role id');
CALL sp_set_column_comment_if_exists('admin_roles', 'code', 'Role code');
CALL sp_set_column_comment_if_exists('admin_roles', 'name', 'Role name');
CALL sp_set_column_comment_if_exists('admin_roles', 'create_time', 'create time');
CALL sp_set_column_comment_if_exists('admin_roles', 'update_time', 'update time');

CALL sp_set_column_comment_if_exists('admin_permissions', 'id', 'Permission id');
CALL sp_set_column_comment_if_exists('admin_permissions', 'code', 'Permission code');
CALL sp_set_column_comment_if_exists('admin_permissions', 'name', 'Permission name');
CALL sp_set_column_comment_if_exists('admin_permissions', 'create_time', 'create time');
CALL sp_set_column_comment_if_exists('admin_permissions', 'update_time', 'update time');

CALL sp_set_column_comment_if_exists('admin_role_permissions', 'id', 'Role permission relation id');
CALL sp_set_column_comment_if_exists('admin_role_permissions', 'role_id', 'Role id');
CALL sp_set_column_comment_if_exists('admin_role_permissions', 'permission_id', 'Permission id');
CALL sp_set_column_comment_if_exists('admin_role_permissions', 'create_time', 'create time');

CALL sp_set_column_comment_if_exists('admin_user_roles', 'id', 'User role relation id');
CALL sp_set_column_comment_if_exists('admin_user_roles', 'user_id', 'User id');
CALL sp_set_column_comment_if_exists('admin_user_roles', 'role_id', 'Role id');
CALL sp_set_column_comment_if_exists('admin_user_roles', 'create_time', 'create time');

-- reports / audit
CALL sp_set_column_comment_if_exists('reports', 'id', 'Report id');
CALL sp_set_column_comment_if_exists('reports', 'reporter_id', 'Reporter user id');
CALL sp_set_column_comment_if_exists('reports', 'target_type', 'Target type: content/comment/user');
CALL sp_set_column_comment_if_exists('reports', 'target_id', 'Target id');
CALL sp_set_column_comment_if_exists('reports', 'reason', 'Report reason');
CALL sp_set_column_comment_if_exists('reports', 'status', 'pending/assigned/resolved/rejected');
CALL sp_set_column_comment_if_exists('reports', 'assignee_id', 'Assignee user id');
CALL sp_set_column_comment_if_exists('reports', 'handle_note', 'Handle note');
CALL sp_set_column_comment_if_exists('reports', 'resolve_action', 'Resolve action');
CALL sp_set_column_comment_if_exists('reports', 'handle_time', 'Handle time');
CALL sp_set_column_comment_if_exists('reports', 'target_snapshot', 'Target snapshot json');
CALL sp_set_column_comment_if_exists('reports', 'create_time', 'create time');
CALL sp_set_column_comment_if_exists('reports', 'update_time', 'update time');

CALL sp_set_column_comment_if_exists('admin_audit_logs', 'id', 'Audit log id');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'operator_user_id', 'Operator user id');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'action', 'Action code');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'target_type', 'Target type');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'target_id', 'Target id');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'detail_before', 'Before snapshot');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'detail_after', 'After snapshot');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'ip', 'Client IP');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'user_agent', 'User agent');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'create_time', 'create time');

-- announcements / templates
CALL sp_set_column_comment_if_exists('announcements', 'id', 'Announcement id');
CALL sp_set_column_comment_if_exists('announcements', 'title', 'Title');
CALL sp_set_column_comment_if_exists('announcements', 'body', 'Body');
CALL sp_set_column_comment_if_exists('announcements', 'status', 'draft/published/offline');
CALL sp_set_column_comment_if_exists('announcements', 'is_pinned', 'Pinned');
CALL sp_set_column_comment_if_exists('announcements', 'start_time', 'Visible start time');
CALL sp_set_column_comment_if_exists('announcements', 'end_time', 'Visible end time');
CALL sp_set_column_comment_if_exists('announcements', 'publish_time', 'Publish time');
CALL sp_set_column_comment_if_exists('announcements', 'creator_id', 'Creator user id');
CALL sp_set_column_comment_if_exists('announcements', 'updater_id', 'Updater user id');
CALL sp_set_column_comment_if_exists('announcements', 'create_time', 'create time');
CALL sp_set_column_comment_if_exists('announcements', 'update_time', 'update time');

CALL sp_set_column_comment_if_exists('announcement_reads', 'id', 'Read record id');
CALL sp_set_column_comment_if_exists('announcement_reads', 'announcement_id', 'Announcement id');
CALL sp_set_column_comment_if_exists('announcement_reads', 'user_id', 'User id');
CALL sp_set_column_comment_if_exists('announcement_reads', 'read_time', 'Read time');
CALL sp_set_column_comment_if_exists('announcement_reads', 'create_time', 'create time');

CALL sp_set_column_comment_if_exists('notification_templates', 'id', 'Template id');
CALL sp_set_column_comment_if_exists('notification_templates', 'code', 'Template code');
CALL sp_set_column_comment_if_exists('notification_templates', 'name', 'Template name');
CALL sp_set_column_comment_if_exists('notification_templates', 'title_template', 'Title template');
CALL sp_set_column_comment_if_exists('notification_templates', 'body_template', 'Body template');
CALL sp_set_column_comment_if_exists('notification_templates', 'status', '1 enabled 0 disabled');
CALL sp_set_column_comment_if_exists('notification_templates', 'create_time', 'create time');
CALL sp_set_column_comment_if_exists('notification_templates', 'update_time', 'update time');

CALL sp_set_column_comment_if_exists('report_violation_templates', 'id', 'Template id');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'code', 'Template code');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'label', 'Template label');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'description', 'Template description');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'status', '1 enabled 0 disabled');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'sort_order', 'Sort order');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'is_system', '1 system 0 custom');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'create_time', 'create time');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'update_time', 'update time');

-- phase15 backup table
CALL sp_set_column_comment_if_exists('content_counter_backup', 'content_id', 'content id');
CALL sp_set_column_comment_if_exists('content_counter_backup', 'like_count', 'old like_count');
CALL sp_set_column_comment_if_exists('content_counter_backup', 'collection_count', 'old collection_count');
CALL sp_set_column_comment_if_exists('content_counter_backup', 'comment_count', 'old comment_count');
CALL sp_set_column_comment_if_exists('content_counter_backup', 'backup_time', 'backup time');

-- table comments
CALL sp_set_table_comment_if_exists('admin_roles', 'Admin roles');
CALL sp_set_table_comment_if_exists('admin_permissions', 'Admin permissions');
CALL sp_set_table_comment_if_exists('admin_role_permissions', 'Admin role permission relation');
CALL sp_set_table_comment_if_exists('admin_user_roles', 'Admin user role relation');
CALL sp_set_table_comment_if_exists('reports', 'Report tickets');
CALL sp_set_table_comment_if_exists('admin_audit_logs', 'Admin audit logs');
CALL sp_set_table_comment_if_exists('announcements', 'Site announcements');
CALL sp_set_table_comment_if_exists('announcement_reads', 'Announcement read records');
CALL sp_set_table_comment_if_exists('notification_templates', 'System notification templates');
CALL sp_set_table_comment_if_exists('report_violation_templates', 'Report violation templates');
CALL sp_set_table_comment_if_exists('content_counter_backup', 'phase15 backup for content counters');

DROP PROCEDURE IF EXISTS sp_set_column_comment_if_exists;
DROP PROCEDURE IF EXISTS sp_set_table_comment_if_exists;

