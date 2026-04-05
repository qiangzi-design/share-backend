-- 数据库索引优化脚本
-- 针对生活分享网站的性能优化

USE share_db;

-- 1. 用户表索引优化
-- username字段用于登录查询，创建唯一索引
CREATE UNIQUE INDEX idx_user_username ON user(username);

-- email字段用于注册和查找，创建唯一索引
CREATE UNIQUE INDEX idx_user_email ON user(email);

-- 2. 内容表索引优化
-- user_id字段用于查询用户发布的内容，创建普通索引
CREATE INDEX idx_content_user_id ON content(user_id);

-- category_id字段用于按分类查询内容，创建普通索引
CREATE INDEX idx_content_category_id ON content(category_id);

-- status字段用于筛选状态，创建普通索引
CREATE INDEX idx_content_status ON content(status);

-- 3. 评论表索引优化
-- content_id字段用于查询内容的评论，创建普通索引
CREATE INDEX idx_comment_content_id ON comment(content_id);

-- user_id字段用于查询用户的评论，创建普通索引
CREATE INDEX idx_comment_user_id ON comment(user_id);

-- 4. 点赞表索引优化
-- content_id字段用于查询内容的点赞，创建普通索引
CREATE INDEX idx_like_content_id ON `like`(content_id);

-- user_id字段用于查询用户的点赞，创建普通索引
CREATE INDEX idx_like_user_id ON `like`(user_id);

-- 联合索引：用于查询用户是否点赞了某内容
CREATE UNIQUE INDEX idx_like_content_user ON `like`(content_id, user_id);

-- 5. 评论点赞表索引优化
-- comment_id字段用于查询评论的点赞，创建普通索引
CREATE INDEX idx_comment_like_comment_id ON comment_like(comment_id);

-- user_id字段用于查询用户的评论点赞，创建普通索引
CREATE INDEX idx_comment_like_user_id ON comment_like(user_id);

-- 联合索引：用于查询用户是否点赞了某评论
CREATE UNIQUE INDEX idx_comment_like_comment_user ON comment_like(comment_id, user_id);

-- 6. 标签表索引优化
-- 标签名称用于查询，创建唯一索引
CREATE UNIQUE INDEX idx_tag_name ON tag(name);

-- 7. 分类表索引优化
-- 分类名称用于查询，创建唯一索引
CREATE UNIQUE INDEX idx_category_name ON category(name);

-- 查看所有索引
SHOW INDEX FROM user;
SHOW INDEX FROM content;
SHOW INDEX FROM comment;
SHOW INDEX FROM `like`;
SHOW INDEX FROM comment_like;
SHOW INDEX FROM tag;
SHOW INDEX FROM category;