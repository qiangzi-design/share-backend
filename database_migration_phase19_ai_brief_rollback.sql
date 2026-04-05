-- Phase 19 rollback: remove daily AI brief module tables
-- MySQL idempotent rollback script

USE share_db;

DROP TABLE IF EXISTS daily_ai_brief_items;
DROP TABLE IF EXISTS daily_ai_briefs;
