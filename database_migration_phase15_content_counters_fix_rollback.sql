-- Phase 15 rollback: restore content counters from phase15 backup table

USE share_db;

UPDATE contents c
INNER JOIN content_counter_fix_backup_phase15 b ON b.content_id = c.id
SET
    c.like_count = b.like_count,
    c.collection_count = b.collection_count,
    c.comment_count = b.comment_count
WHERE
    IFNULL(c.like_count, 0) <> IFNULL(b.like_count, 0)
    OR IFNULL(c.collection_count, 0) <> IFNULL(b.collection_count, 0)
    OR IFNULL(c.comment_count, 0) <> IFNULL(b.comment_count, 0);

