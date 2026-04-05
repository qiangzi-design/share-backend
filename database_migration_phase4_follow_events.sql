-- Phase 4 manual migration: follow/unfollow event log for statistics
USE share_db;

CREATE TABLE IF NOT EXISTS follow_events (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '事件ID',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    target_user_id BIGINT NOT NULL COMMENT '被关注用户ID',
    event_type TINYINT NOT NULL COMMENT '事件类型：1-关注，2-取关',
    create_time TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
    PRIMARY KEY (id),
    KEY idx_follow_events_target_time (target_user_id, create_time),
    KEY idx_follow_events_target_type_time (target_user_id, event_type, create_time),
    KEY idx_follow_events_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Backfill current follows as historical follow events (idempotent by event type uniqueness check)
INSERT INTO follow_events (user_id, target_user_id, event_type, create_time)
SELECT f.user_id, f.target_user_id, 1, COALESCE(f.create_time, NOW())
FROM follows f
LEFT JOIN follow_events e
       ON e.user_id = f.user_id
      AND e.target_user_id = f.target_user_id
      AND e.event_type = 1
WHERE e.id IS NULL;
