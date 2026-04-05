-- Phase 7: emoji support for comments and private chat
-- Ensure 4-byte UTF-8 storage in MySQL

ALTER TABLE comments
    CONVERT TO CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

ALTER TABLE chat_messages
    CONVERT TO CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
