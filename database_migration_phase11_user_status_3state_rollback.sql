-- Phase 11 rollback: revert user status to legacy 2 states
-- legacy: 0 = disabled, 1 = normal

UPDATE users
SET status = 0
WHERE status = 3;

UPDATE users
SET status = 1
WHERE status = 2;

UPDATE users
SET status = 1
WHERE status IS NULL;

ALTER TABLE users
    MODIFY COLUMN status INT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常';
