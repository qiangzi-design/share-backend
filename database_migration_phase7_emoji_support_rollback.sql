-- Phase 7 rollback: emoji support
-- Warning: if data already contains 4-byte emoji characters, this rollback may fail.

ALTER TABLE comments
    CONVERT TO CHARACTER SET utf8mb3
    COLLATE utf8mb3_general_ci;

ALTER TABLE chat_messages
    CONVERT TO CHARACTER SET utf8mb3
    COLLATE utf8mb3_general_ci;
