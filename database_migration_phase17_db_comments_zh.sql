-- Phase 17: normalize English DB comments to Chinese (table/column comments only)
-- MySQL idempotent migration script

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

-- =========================
-- 1) users / contents / comments
-- =========================
CALL sp_set_column_comment_if_exists('users', 'mute_until', '禁言截止时间');
CALL sp_set_column_comment_if_exists('users', 'ban_reason', '封禁原因');
CALL sp_set_column_comment_if_exists('users', 'ban_time', '封禁时间');
CALL sp_set_column_comment_if_exists('users', 'risk_level', '风险等级（low/medium/high）');
CALL sp_set_column_comment_if_exists('users', 'risk_note', '风险备注');
CALL sp_set_column_comment_if_exists('users', 'risk_mark_time', '风险标记时间');
CALL sp_set_column_comment_if_exists('users', 'risk_mark_by', '风险标记人用户ID');

CALL sp_set_column_comment_if_exists('contents', 'review_status', '审核状态');
CALL sp_set_column_comment_if_exists('contents', 'review_reason', '审核说明');
CALL sp_set_column_comment_if_exists('contents', 'reviewer_id', '审核人用户ID');
CALL sp_set_column_comment_if_exists('contents', 'review_time', '审核时间');
CALL sp_set_column_comment_if_exists('contents', 'videos', '视频地址列表（逗号分隔）');

CALL sp_set_column_comment_if_exists('comments', 'review_status', '审核状态');
CALL sp_set_column_comment_if_exists('comments', 'review_reason', '审核说明');
CALL sp_set_column_comment_if_exists('comments', 'reviewer_id', '审核人用户ID');
CALL sp_set_column_comment_if_exists('comments', 'review_time', '审核时间');

-- =========================
-- 2) RBAC
-- =========================
CALL sp_set_column_comment_if_exists('admin_roles', 'id', '角色ID');
CALL sp_set_column_comment_if_exists('admin_roles', 'code', '角色编码');
CALL sp_set_column_comment_if_exists('admin_roles', 'name', '角色名称');
CALL sp_set_column_comment_if_exists('admin_roles', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('admin_roles', 'update_time', '更新时间');

CALL sp_set_column_comment_if_exists('admin_permissions', 'id', '权限ID');
CALL sp_set_column_comment_if_exists('admin_permissions', 'code', '权限编码');
CALL sp_set_column_comment_if_exists('admin_permissions', 'name', '权限名称');
CALL sp_set_column_comment_if_exists('admin_permissions', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('admin_permissions', 'update_time', '更新时间');

CALL sp_set_column_comment_if_exists('admin_role_permissions', 'id', '角色权限关联ID');
CALL sp_set_column_comment_if_exists('admin_role_permissions', 'role_id', '角色ID');
CALL sp_set_column_comment_if_exists('admin_role_permissions', 'permission_id', '权限ID');
CALL sp_set_column_comment_if_exists('admin_role_permissions', 'create_time', '创建时间');

CALL sp_set_column_comment_if_exists('admin_user_roles', 'id', '用户角色关联ID');
CALL sp_set_column_comment_if_exists('admin_user_roles', 'user_id', '用户ID');
CALL sp_set_column_comment_if_exists('admin_user_roles', 'role_id', '角色ID');
CALL sp_set_column_comment_if_exists('admin_user_roles', 'create_time', '创建时间');

-- =========================
-- 3) reports / audit
-- =========================
CALL sp_set_column_comment_if_exists('reports', 'id', '举报工单ID');
CALL sp_set_column_comment_if_exists('reports', 'reporter_id', '举报人用户ID');
CALL sp_set_column_comment_if_exists('reports', 'target_type', '目标类型（content/comment/user）');
CALL sp_set_column_comment_if_exists('reports', 'target_id', '目标ID');
CALL sp_set_column_comment_if_exists('reports', 'reason', '举报原因');
CALL sp_set_column_comment_if_exists('reports', 'status', '工单状态（pending/assigned/resolved/rejected）');
CALL sp_set_column_comment_if_exists('reports', 'assignee_id', '处理人用户ID');
CALL sp_set_column_comment_if_exists('reports', 'handle_note', '处理备注');
CALL sp_set_column_comment_if_exists('reports', 'resolve_action', '处理动作');
CALL sp_set_column_comment_if_exists('reports', 'handle_time', '处理时间');
CALL sp_set_column_comment_if_exists('reports', 'target_snapshot', '目标快照JSON');
CALL sp_set_column_comment_if_exists('reports', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('reports', 'update_time', '更新时间');

CALL sp_set_column_comment_if_exists('admin_audit_logs', 'id', '审计日志ID');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'operator_user_id', '操作者用户ID');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'action', '操作动作编码');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'target_type', '目标类型');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'target_id', '目标ID');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'detail_before', '变更前快照');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'detail_after', '变更后快照');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'ip', '客户端IP');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'user_agent', '客户端UA');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'create_time', '创建时间');

-- =========================
-- 4) announcements / templates
-- =========================
CALL sp_set_column_comment_if_exists('announcements', 'id', '公告ID');
CALL sp_set_column_comment_if_exists('announcements', 'title', '公告标题');
CALL sp_set_column_comment_if_exists('announcements', 'body', '公告正文');
CALL sp_set_column_comment_if_exists('announcements', 'status', '公告状态（draft/published/offline）');
CALL sp_set_column_comment_if_exists('announcements', 'is_pinned', '是否置顶');
CALL sp_set_column_comment_if_exists('announcements', 'start_time', '生效开始时间');
CALL sp_set_column_comment_if_exists('announcements', 'end_time', '生效结束时间');
CALL sp_set_column_comment_if_exists('announcements', 'publish_time', '发布时间');
CALL sp_set_column_comment_if_exists('announcements', 'creator_id', '创建人用户ID');
CALL sp_set_column_comment_if_exists('announcements', 'updater_id', '更新人用户ID');
CALL sp_set_column_comment_if_exists('announcements', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('announcements', 'update_time', '更新时间');

CALL sp_set_column_comment_if_exists('announcement_reads', 'id', '已读记录ID');
CALL sp_set_column_comment_if_exists('announcement_reads', 'announcement_id', '公告ID');
CALL sp_set_column_comment_if_exists('announcement_reads', 'user_id', '用户ID');
CALL sp_set_column_comment_if_exists('announcement_reads', 'read_time', '已读时间');
CALL sp_set_column_comment_if_exists('announcement_reads', 'create_time', '创建时间');

CALL sp_set_column_comment_if_exists('notification_templates', 'id', '模板ID');
CALL sp_set_column_comment_if_exists('notification_templates', 'code', '模板编码');
CALL sp_set_column_comment_if_exists('notification_templates', 'name', '模板名称');
CALL sp_set_column_comment_if_exists('notification_templates', 'title_template', '标题模板');
CALL sp_set_column_comment_if_exists('notification_templates', 'body_template', '正文模板');
CALL sp_set_column_comment_if_exists('notification_templates', 'status', '启用状态（1启用0停用）');
CALL sp_set_column_comment_if_exists('notification_templates', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('notification_templates', 'update_time', '更新时间');

CALL sp_set_column_comment_if_exists('report_violation_templates', 'id', '模板ID');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'code', '模板编码');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'label', '模板标签');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'description', '模板描述');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'status', '启用状态（1启用0停用）');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'sort_order', '排序值');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'is_system', '是否系统模板（1系统0自定义）');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'update_time', '更新时间');

-- =========================
-- 5) phase15 backup table (if exists)
-- =========================
CALL sp_set_column_comment_if_exists('content_counter_backup', 'content_id', '内容ID');
CALL sp_set_column_comment_if_exists('content_counter_backup', 'like_count', '旧点赞数');
CALL sp_set_column_comment_if_exists('content_counter_backup', 'collection_count', '旧收藏数');
CALL sp_set_column_comment_if_exists('content_counter_backup', 'comment_count', '旧评论数');
CALL sp_set_column_comment_if_exists('content_counter_backup', 'backup_time', '备份时间');

-- =========================
-- 6) table comments
-- =========================
CALL sp_set_table_comment_if_exists('admin_roles', '管理角色表');
CALL sp_set_table_comment_if_exists('admin_permissions', '管理权限表');
CALL sp_set_table_comment_if_exists('admin_role_permissions', '角色权限关联表');
CALL sp_set_table_comment_if_exists('admin_user_roles', '用户角色关联表');
CALL sp_set_table_comment_if_exists('reports', '举报工单表');
CALL sp_set_table_comment_if_exists('admin_audit_logs', '管理操作审计日志表');
CALL sp_set_table_comment_if_exists('announcements', '站内公告表');
CALL sp_set_table_comment_if_exists('announcement_reads', '公告已读记录表');
CALL sp_set_table_comment_if_exists('notification_templates', '系统通知模板表');
CALL sp_set_table_comment_if_exists('report_violation_templates', '举报违规模板表');
CALL sp_set_table_comment_if_exists('content_counter_backup', '内容计数修复备份表');

DROP PROCEDURE IF EXISTS sp_set_column_comment_if_exists;
DROP PROCEDURE IF EXISTS sp_set_table_comment_if_exists;

