-- Phase 13: report violation templates (db-driven, not hardcoded)
-- MySQL idempotent migration script

USE share_db;

CREATE TABLE IF NOT EXISTS report_violation_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Template id',
    code VARCHAR(64) NOT NULL COMMENT 'Template code',
    label VARCHAR(80) NOT NULL COMMENT 'Template label',
    description VARCHAR(255) NULL COMMENT 'Template description',
    status TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 enabled 0 disabled',
    sort_order INT NOT NULL DEFAULT 0 COMMENT 'Sort order',
    is_system TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1 system 0 custom',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Report violation templates'
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

SET @has_uk_report_templates_code := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_violation_templates' AND INDEX_NAME = 'uk_report_templates_code'
);
SET @sql := IF(
    @has_uk_report_templates_code = 0,
    'CREATE UNIQUE INDEX uk_report_templates_code ON report_violation_templates(code)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx_report_templates_status_sort := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_violation_templates' AND INDEX_NAME = 'idx_report_templates_status_sort'
);
SET @sql := IF(
    @has_idx_report_templates_status_sort = 0,
    'CREATE INDEX idx_report_templates_status_sort ON report_violation_templates(status, sort_order, id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO report_violation_templates(code, label, description, status, sort_order, is_system)
VALUES
    ('pornography', '色情低俗', '包含色情暗示、低俗露骨内容', 1, 10, 1),
    ('violence', '血腥暴力', '包含血腥画面、暴力鼓吹内容', 1, 20, 1),
    ('subversion', '反动言论', '包含煽动颠覆、破坏公共秩序言论', 1, 30, 1),
    ('extremism', '极端言论', '包含极端主义、仇恨煽动内容', 1, 40, 1),
    ('borderline', '擦边低俗', '存在擦边内容、明显不适宜传播', 1, 50, 1),
    ('gender_conflict', '性别对立', '恶意制造性别对立、群体攻击', 1, 60, 1),
    ('anti_learning', '读书无用论', '恶意传播学习无价值等误导内容', 1, 70, 1),
    ('abusive', '弱智发言', '侮辱性、恶意贬损与低质攻击内容', 1, 80, 1),
    ('spam_ad', '广告引流', '含广告导流、恶意推广、刷屏信息', 1, 90, 1),
    ('rumor', '谣言虚假', '传播未经证实或明显虚假信息', 1, 100, 1),
    ('privacy', '隐私泄露', '泄露他人隐私、身份信息或联系方式', 1, 110, 1)
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    description = VALUES(description),
    status = VALUES(status),
    sort_order = VALUES(sort_order),
    is_system = VALUES(is_system),
    update_time = CURRENT_TIMESTAMP;
