-- Phase 18: sync all table/column comments from share_db.sql (non-destructive)
-- Purpose: only update COMMENT metadata for existing tables/columns.

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
    DECLARE v_extra_clean TEXT;
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

        -- MySQL 8 information_schema.EXTRA 可能包含 DEFAULT_GENERATED，
        -- 该标记不能直接出现在 ALTER ... MODIFY 语句里，需先过滤。
        SET v_extra_clean = TRIM(
            REPLACE(
                REPLACE(
                    REPLACE(IFNULL(v_extra, ''), 'DEFAULT_GENERATED', ''),
                    'VIRTUAL GENERATED', ''
                ),
                'STORED GENERATED', ''
            )
        );

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
            CASE WHEN v_extra_clean IS NOT NULL AND v_extra_clean <> '' THEN CONCAT(' ', v_extra_clean) ELSE '' END,
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

-- Column comments
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'action', '操作编码');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'detail_after', '变更后快照');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'detail_before', '变更前快照');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'id', '审计日志ID');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'ip', '客户端IP');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'operator_user_id', '操作者用户ID');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'target_id', '目标ID');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'target_type', '目标类型');
CALL sp_set_column_comment_if_exists('admin_audit_logs', 'user_agent', '客户端UA');
CALL sp_set_column_comment_if_exists('admin_permissions', 'code', '权限编码');
CALL sp_set_column_comment_if_exists('admin_permissions', 'id', '权限ID');
CALL sp_set_column_comment_if_exists('admin_permissions', 'name', '权限名称');
CALL sp_set_column_comment_if_exists('admin_role_permissions', 'id', '角色权限关联ID');
CALL sp_set_column_comment_if_exists('admin_role_permissions', 'permission_id', '权限ID');
CALL sp_set_column_comment_if_exists('admin_role_permissions', 'role_id', '角色ID');
CALL sp_set_column_comment_if_exists('admin_roles', 'code', '角色编码');
CALL sp_set_column_comment_if_exists('admin_roles', 'id', '角色ID');
CALL sp_set_column_comment_if_exists('admin_roles', 'name', '角色名称');
CALL sp_set_column_comment_if_exists('admin_user_roles', 'id', '用户角色关联ID');
CALL sp_set_column_comment_if_exists('admin_user_roles', 'role_id', '角色ID');
CALL sp_set_column_comment_if_exists('admin_user_roles', 'user_id', '用户ID');
CALL sp_set_column_comment_if_exists('announcement_reads', 'announcement_id', '公告ID');
CALL sp_set_column_comment_if_exists('announcement_reads', 'id', '已读记录ID');
CALL sp_set_column_comment_if_exists('announcement_reads', 'read_time', '已读时间');
CALL sp_set_column_comment_if_exists('announcement_reads', 'user_id', '用户ID');
CALL sp_set_column_comment_if_exists('announcements', 'body', '正文');
CALL sp_set_column_comment_if_exists('announcements', 'creator_id', '创建人用户ID');
CALL sp_set_column_comment_if_exists('announcements', 'end_time', '生效结束时间');
CALL sp_set_column_comment_if_exists('announcements', 'id', '公告ID');
CALL sp_set_column_comment_if_exists('announcements', 'is_pinned', '是否置顶');
CALL sp_set_column_comment_if_exists('announcements', 'publish_time', '发布时间');
CALL sp_set_column_comment_if_exists('announcements', 'start_time', '生效开始时间');
CALL sp_set_column_comment_if_exists('announcements', 'status', '状态：草稿/已发布/已下线');
CALL sp_set_column_comment_if_exists('announcements', 'title', '标题');
CALL sp_set_column_comment_if_exists('announcements', 'updater_id', '更新人用户ID');
CALL sp_set_column_comment_if_exists('categories', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('categories', 'description', '分类描述');
CALL sp_set_column_comment_if_exists('categories', 'id', '分类ID');
CALL sp_set_column_comment_if_exists('categories', 'name', '分类名称');
CALL sp_set_column_comment_if_exists('categories', 'sort_order', '排序权重');
CALL sp_set_column_comment_if_exists('categories', 'status', '状态：0-禁用，1-正常');
CALL sp_set_column_comment_if_exists('categories', 'update_time', '更新时间');
CALL sp_set_column_comment_if_exists('chat_conversations', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('chat_conversations', 'id', '会话ID');
CALL sp_set_column_comment_if_exists('chat_conversations', 'last_message_id', '最后一条消息ID');
CALL sp_set_column_comment_if_exists('chat_conversations', 'last_message_time', '最后消息时间');
CALL sp_set_column_comment_if_exists('chat_conversations', 'update_time', '更新时间');
CALL sp_set_column_comment_if_exists('chat_conversations', 'user_high_id', '较大用户ID');
CALL sp_set_column_comment_if_exists('chat_conversations', 'user_low_id', '较小用户ID');
CALL sp_set_column_comment_if_exists('chat_messages', 'content', '消息内容');
CALL sp_set_column_comment_if_exists('chat_messages', 'conversation_id', '会话ID');
CALL sp_set_column_comment_if_exists('chat_messages', 'create_time', '发送时间');
CALL sp_set_column_comment_if_exists('chat_messages', 'id', '消息ID');
CALL sp_set_column_comment_if_exists('chat_messages', 'is_read', '是否已读');
CALL sp_set_column_comment_if_exists('chat_messages', 'message_type', '消息类型：1文本');
CALL sp_set_column_comment_if_exists('chat_messages', 'read_time', '已读时间');
CALL sp_set_column_comment_if_exists('chat_messages', 'receiver_id', '接收者ID');
CALL sp_set_column_comment_if_exists('chat_messages', 'sender_id', '发送者ID');
CALL sp_set_column_comment_if_exists('chat_oneway_quota', 'id', '单向消息配额ID');
CALL sp_set_column_comment_if_exists('chat_oneway_quota', 'receiver_id', '接收者ID');
CALL sp_set_column_comment_if_exists('chat_oneway_quota', 'sender_id', '发送者ID');
CALL sp_set_column_comment_if_exists('chat_oneway_quota', 'used_at', '首条消息使用时间');
CALL sp_set_column_comment_if_exists('collections', 'content_id', '内容ID');
CALL sp_set_column_comment_if_exists('collections', 'create_time', '收藏时间');
CALL sp_set_column_comment_if_exists('collections', 'id', '收藏ID');
CALL sp_set_column_comment_if_exists('collections', 'user_id', '用户ID');
CALL sp_set_column_comment_if_exists('comment_likes', 'comment_id', '评论ID');
CALL sp_set_column_comment_if_exists('comment_likes', 'create_time', '点赞时间');
CALL sp_set_column_comment_if_exists('comment_likes', 'id', '评论点赞ID');
CALL sp_set_column_comment_if_exists('comment_likes', 'user_id', '用户ID');
CALL sp_set_column_comment_if_exists('comments', 'comment_content', '评论内容');
CALL sp_set_column_comment_if_exists('comments', 'content_id', '内容ID');
CALL sp_set_column_comment_if_exists('comments', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('comments', 'id', '评论ID');
CALL sp_set_column_comment_if_exists('comments', 'like_count', '点赞次数');
CALL sp_set_column_comment_if_exists('comments', 'parent_id', '父评论ID（空表示顶级评论）');
CALL sp_set_column_comment_if_exists('comments', 'review_reason', '审核说明');
CALL sp_set_column_comment_if_exists('comments', 'review_status', '审核状态');
CALL sp_set_column_comment_if_exists('comments', 'review_time', '审核时间');
CALL sp_set_column_comment_if_exists('comments', 'reviewer_id', '审核人用户ID');
CALL sp_set_column_comment_if_exists('comments', 'status', '状态：0-删除，1-正常');
CALL sp_set_column_comment_if_exists('comments', 'update_time', '更新时间');
CALL sp_set_column_comment_if_exists('comments', 'user_id', '评论者ID');
CALL sp_set_column_comment_if_exists('content_counter_fix_backup_phase15', 'backup_time', '备份时间');
CALL sp_set_column_comment_if_exists('content_counter_fix_backup_phase15', 'collection_count', '旧收藏数');
CALL sp_set_column_comment_if_exists('content_counter_fix_backup_phase15', 'comment_count', '旧评论数');
CALL sp_set_column_comment_if_exists('content_counter_fix_backup_phase15', 'content_id', '内容ID');
CALL sp_set_column_comment_if_exists('content_counter_fix_backup_phase15', 'like_count', '旧点赞数');
CALL sp_set_column_comment_if_exists('content_view_events', 'content_id', '内容ID');
CALL sp_set_column_comment_if_exists('content_view_events', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('content_view_events', 'id', '浏览事件ID');
CALL sp_set_column_comment_if_exists('content_view_events', 'ip', '客户端IP');
CALL sp_set_column_comment_if_exists('content_view_events', 'user_agent', '客户端UA');
CALL sp_set_column_comment_if_exists('content_view_events', 'user_id', '浏览用户ID（未登录为空）');
CALL sp_set_column_comment_if_exists('content_view_events', 'viewer_key', '浏览器标识（匿名去重）');
CALL sp_set_column_comment_if_exists('contents', 'category_id', '分类ID');
CALL sp_set_column_comment_if_exists('contents', 'collection_count', '收藏次数');
CALL sp_set_column_comment_if_exists('contents', 'comment_count', '评论次数');
CALL sp_set_column_comment_if_exists('contents', 'content', '内容');
CALL sp_set_column_comment_if_exists('contents', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('contents', 'id', '内容ID');
CALL sp_set_column_comment_if_exists('contents', 'image_size', '图片总大小（字节）');
CALL sp_set_column_comment_if_exists('contents', 'images', '图片列表（逗号分隔）');
CALL sp_set_column_comment_if_exists('contents', 'like_count', '点赞次数');
CALL sp_set_column_comment_if_exists('contents', 'review_reason', '审核说明');
CALL sp_set_column_comment_if_exists('contents', 'review_status', '审核状态');
CALL sp_set_column_comment_if_exists('contents', 'review_time', '审核时间');
CALL sp_set_column_comment_if_exists('contents', 'reviewer_id', '审核人用户ID');
CALL sp_set_column_comment_if_exists('contents', 'status', '状态：0-草稿，1-发布，2-删除');
CALL sp_set_column_comment_if_exists('contents', 'tags', '标签列表（逗号分隔）');
CALL sp_set_column_comment_if_exists('contents', 'title', '标题');
CALL sp_set_column_comment_if_exists('contents', 'update_time', '更新时间');
CALL sp_set_column_comment_if_exists('contents', 'user_id', '发布者ID');
CALL sp_set_column_comment_if_exists('contents', 'videos', '视频地址列表（逗号分隔）');
CALL sp_set_column_comment_if_exists('contents', 'view_count', '浏览次数');
CALL sp_set_column_comment_if_exists('follow_events', 'create_time', '事件时间');
CALL sp_set_column_comment_if_exists('follow_events', 'event_type', '事件类型：1-关注，2-取关');
CALL sp_set_column_comment_if_exists('follow_events', 'id', '事件ID');
CALL sp_set_column_comment_if_exists('follow_events', 'target_user_id', '被关注用户ID');
CALL sp_set_column_comment_if_exists('follow_events', 'user_id', '操作用户ID');
CALL sp_set_column_comment_if_exists('follows', 'create_time', '关注时间');
CALL sp_set_column_comment_if_exists('follows', 'id', '关注关系ID');
CALL sp_set_column_comment_if_exists('follows', 'target_user_id', '被关注用户ID');
CALL sp_set_column_comment_if_exists('follows', 'user_id', '关注者ID');
CALL sp_set_column_comment_if_exists('likes', 'content_id', '内容ID');
CALL sp_set_column_comment_if_exists('likes', 'create_time', '点赞时间');
CALL sp_set_column_comment_if_exists('likes', 'id', '点赞ID');
CALL sp_set_column_comment_if_exists('likes', 'user_id', '用户ID');
CALL sp_set_column_comment_if_exists('notification_templates', 'body_template', '正文模板');
CALL sp_set_column_comment_if_exists('notification_templates', 'code', '模板编码');
CALL sp_set_column_comment_if_exists('notification_templates', 'id', '模板ID');
CALL sp_set_column_comment_if_exists('notification_templates', 'name', '模板名称');
CALL sp_set_column_comment_if_exists('notification_templates', 'status', '1启用0停用');
CALL sp_set_column_comment_if_exists('notification_templates', 'title_template', '标题模板');
CALL sp_set_column_comment_if_exists('notifications', 'actor_id', '触发者用户ID');
CALL sp_set_column_comment_if_exists('notifications', 'body', '通知内容');
CALL sp_set_column_comment_if_exists('notifications', 'content', '通知内容');
CALL sp_set_column_comment_if_exists('notifications', 'content_id', '关联内容ID');
CALL sp_set_column_comment_if_exists('notifications', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('notifications', 'id', '通知ID');
CALL sp_set_column_comment_if_exists('notifications', 'is_read', '是否已读');
CALL sp_set_column_comment_if_exists('notifications', 'read_time', '已读时间');
CALL sp_set_column_comment_if_exists('notifications', 'receiver_id', '接收者用户ID');
CALL sp_set_column_comment_if_exists('notifications', 'related_id', '关联ID（评论ID或内容ID）');
CALL sp_set_column_comment_if_exists('notifications', 'related_user_id', '关联用户ID');
CALL sp_set_column_comment_if_exists('notifications', 'status', '状态：0-未读，1-已读');
CALL sp_set_column_comment_if_exists('notifications', 'title', '通知标题');
CALL sp_set_column_comment_if_exists('notifications', 'type', '通知类型');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'code', '模板编码');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'description', '模板描述');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'id', '模板ID');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'is_system', '1系统模板0自定义模板');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'label', '模板标签');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'sort_order', '排序值');
CALL sp_set_column_comment_if_exists('report_violation_templates', 'status', '1启用0停用');
CALL sp_set_column_comment_if_exists('reports', 'assignee_id', '处理人用户ID');
CALL sp_set_column_comment_if_exists('reports', 'handle_note', '处理备注');
CALL sp_set_column_comment_if_exists('reports', 'handle_time', '处理时间');
CALL sp_set_column_comment_if_exists('reports', 'id', '举报工单ID');
CALL sp_set_column_comment_if_exists('reports', 'reason', '举报原因');
CALL sp_set_column_comment_if_exists('reports', 'reporter_id', '举报人用户ID');
CALL sp_set_column_comment_if_exists('reports', 'resolve_action', '处理动作');
CALL sp_set_column_comment_if_exists('reports', 'status', '状态：待处理/处理中/已处理/已驳回');
CALL sp_set_column_comment_if_exists('reports', 'target_id', '目标ID');
CALL sp_set_column_comment_if_exists('reports', 'target_snapshot', '目标快照（结构化数据）');
CALL sp_set_column_comment_if_exists('reports', 'target_type', '目标类型：内容/评论/用户');
CALL sp_set_column_comment_if_exists('tags', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('tags', 'id', '标签ID');
CALL sp_set_column_comment_if_exists('tags', 'name', '标签名称');
CALL sp_set_column_comment_if_exists('tags', 'status', '状态：0-禁用，1-正常');
CALL sp_set_column_comment_if_exists('tags', 'update_time', '更新时间');
CALL sp_set_column_comment_if_exists('tags', 'use_count', '使用次数');
CALL sp_set_column_comment_if_exists('users', 'avatar', '头像地址');
CALL sp_set_column_comment_if_exists('users', 'ban_reason', '封禁原因');
CALL sp_set_column_comment_if_exists('users', 'ban_time', '封禁时间');
CALL sp_set_column_comment_if_exists('users', 'bio', '个人简介');
CALL sp_set_column_comment_if_exists('users', 'create_time', '创建时间');
CALL sp_set_column_comment_if_exists('users', 'email', '邮箱');
CALL sp_set_column_comment_if_exists('users', 'id', '用户ID');
CALL sp_set_column_comment_if_exists('users', 'mute_until', '禁言截止时间');
CALL sp_set_column_comment_if_exists('users', 'nickname', '昵称');
CALL sp_set_column_comment_if_exists('users', 'password', '密码（加密存储）');
CALL sp_set_column_comment_if_exists('users', 'risk_level', '风险等级：低/中/高');
CALL sp_set_column_comment_if_exists('users', 'risk_mark_by', '风险标记人用户ID');
CALL sp_set_column_comment_if_exists('users', 'risk_mark_time', '风险标记时间');
CALL sp_set_column_comment_if_exists('users', 'risk_note', '风险备注');
CALL sp_set_column_comment_if_exists('users', 'status', '状态：1-正常，2-禁言，3-封禁');
CALL sp_set_column_comment_if_exists('users', 'update_time', '更新时间');
CALL sp_set_column_comment_if_exists('users', 'username', '用户名');

-- Table comments
CALL sp_set_table_comment_if_exists('admin_audit_logs', '管理操作审计日志表');
CALL sp_set_table_comment_if_exists('admin_permissions', '管理权限表');
CALL sp_set_table_comment_if_exists('admin_role_permissions', '角色权限关联表');
CALL sp_set_table_comment_if_exists('admin_roles', '管理角色表');
CALL sp_set_table_comment_if_exists('admin_user_roles', '用户角色关联表');
CALL sp_set_table_comment_if_exists('announcement_reads', '公告已读记录表');
CALL sp_set_table_comment_if_exists('announcements', '站内公告表');
CALL sp_set_table_comment_if_exists('chat_conversations', '私聊会话表');
CALL sp_set_table_comment_if_exists('chat_messages', '私聊消息表');
CALL sp_set_table_comment_if_exists('chat_oneway_quota', '单向关注私聊配额表');
CALL sp_set_table_comment_if_exists('content_counter_fix_backup_phase15', '内容计数修复备份表（第15阶段）');
CALL sp_set_table_comment_if_exists('content_view_events', '内容浏览事件表');
CALL sp_set_table_comment_if_exists('notification_templates', '系统通知模板表');
CALL sp_set_table_comment_if_exists('report_violation_templates', '举报违规模板表');
CALL sp_set_table_comment_if_exists('reports', '举报工单表');

DROP PROCEDURE IF EXISTS sp_set_column_comment_if_exists;
DROP PROCEDURE IF EXISTS sp_set_table_comment_if_exists;
