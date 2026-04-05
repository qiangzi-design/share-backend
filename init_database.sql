-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS share_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 切换到share_db数据库
USE share_db;

-- 显示数据库创建信息
SELECT 'Database share_db created successfully!' AS message;