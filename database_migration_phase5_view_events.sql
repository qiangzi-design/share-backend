-- Phase 5: content view event tracking
-- Purpose: support deduplicated view counting rules

CREATE TABLE IF NOT EXISTS content_view_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '浏览事件ID',
    content_id BIGINT NOT NULL COMMENT '内容ID',
    user_id BIGINT NULL COMMENT '浏览用户ID（未登录为空）',
    viewer_key VARCHAR(64) NOT NULL COMMENT '浏览器标识（匿名去重）',
    ip VARCHAR(64) NULL COMMENT '客户端IP',
    user_agent VARCHAR(255) NULL COMMENT '客户端UA',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='内容浏览事件表';

CREATE INDEX idx_cve_content_time ON content_view_events(content_id, create_time);
CREATE INDEX idx_cve_user_content_time ON content_view_events(user_id, content_id, create_time);
CREATE INDEX idx_cve_viewer_content_time ON content_view_events(viewer_key, content_id, create_time);
