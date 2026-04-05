-- Phase 15: repair content interaction counters from detail tables
-- Safe to run multiple times.

USE share_db;

-- 1) Backup current counters before recalculation.
CREATE TABLE IF NOT EXISTS content_counter_fix_backup_phase15 (
    content_id BIGINT PRIMARY KEY COMMENT 'content id',
    like_count INT NOT NULL DEFAULT 0 COMMENT 'old like_count',
    collection_count INT NOT NULL DEFAULT 0 COMMENT 'old collection_count',
    comment_count INT NOT NULL DEFAULT 0 COMMENT 'old comment_count',
    backup_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'backup time'
) COMMENT='phase15 backup for content counters'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO content_counter_fix_backup_phase15 (content_id, like_count, collection_count, comment_count, backup_time)
SELECT
    c.id AS content_id,
    IFNULL(c.like_count, 0) AS like_count,
    IFNULL(c.collection_count, 0) AS collection_count,
    IFNULL(c.comment_count, 0) AS comment_count,
    NOW() AS backup_time
FROM contents c
ON DUPLICATE KEY UPDATE
    like_count = VALUES(like_count),
    collection_count = VALUES(collection_count),
    comment_count = VALUES(comment_count),
    backup_time = VALUES(backup_time);

-- 2) Recalculate counters from source-of-truth detail tables.
UPDATE contents c
LEFT JOIN (
    SELECT l.content_id, COUNT(*) AS cnt
    FROM likes l
    GROUP BY l.content_id
) lc ON lc.content_id = c.id
LEFT JOIN (
    SELECT cl.content_id, COUNT(*) AS cnt
    FROM collections cl
    GROUP BY cl.content_id
) cc ON cc.content_id = c.id
LEFT JOIN (
    SELECT cm.content_id, COUNT(*) AS cnt
    FROM comments cm
    WHERE cm.parent_id IS NULL
      AND cm.status = 1
      AND (cm.review_status = 'approved' OR cm.review_status IS NULL)
    GROUP BY cm.content_id
) mc ON mc.content_id = c.id
SET
    c.like_count = IFNULL(lc.cnt, 0),
    c.collection_count = IFNULL(cc.cnt, 0),
    c.comment_count = IFNULL(mc.cnt, 0)
WHERE
    IFNULL(c.like_count, 0) <> IFNULL(lc.cnt, 0)
    OR IFNULL(c.collection_count, 0) <> IFNULL(cc.cnt, 0)
    OR IFNULL(c.comment_count, 0) <> IFNULL(mc.cnt, 0);

