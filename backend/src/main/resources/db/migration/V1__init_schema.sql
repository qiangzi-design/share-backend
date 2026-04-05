-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    bio TEXT COMMENT '个人简介',
    status INT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 创建索引
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- 创建分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称',
    description VARCHAR(255) COMMENT '分类描述',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    status INT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 创建索引
CREATE INDEX idx_categories_sort_order ON categories(sort_order);

-- 创建标签表
CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name VARCHAR(30) NOT NULL UNIQUE COMMENT '标签名称',
    use_count INT DEFAULT 0 COMMENT '使用次数',
    status INT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 创建索引
CREATE INDEX idx_tags_name ON tags(name);

-- 创建内容表
CREATE TABLE IF NOT EXISTS contents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '内容ID',
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    category_id BIGINT COMMENT '分类ID',
    tags VARCHAR(255) COMMENT '标签列表（逗号分隔）',
    images VARCHAR(1000) COMMENT '图片列表（逗号分隔）',
    image_size BIGINT DEFAULT 0 COMMENT '图片总大小（字节）',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    like_count INT DEFAULT 0 COMMENT '点赞次数',
    comment_count INT DEFAULT 0 COMMENT '评论次数',
    status INT DEFAULT 1 COMMENT '状态：0-草稿，1-发布，2-删除',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 创建索引
CREATE INDEX idx_contents_user_id ON contents(user_id);
CREATE INDEX idx_contents_category_id ON contents(category_id);
CREATE INDEX idx_contents_create_time ON contents(create_time);
CREATE INDEX idx_contents_status ON contents(status);

-- 创建点赞表
CREATE TABLE IF NOT EXISTS likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content_id BIGINT NOT NULL COMMENT '内容ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE (user_id, content_id)
);

-- 创建评论表
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    user_id BIGINT NOT NULL COMMENT '评论者ID',
    content_id BIGINT NOT NULL COMMENT '内容ID',
    parent_id BIGINT COMMENT '父评论ID（null表示顶级评论）',
    comment_content TEXT NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0 COMMENT '点赞次数',
    status INT DEFAULT 1 COMMENT '状态：0-删除，1-正常',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
);

-- 创建索引
CREATE INDEX idx_comments_content_id ON comments(content_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);

-- 创建评论点赞表
CREATE TABLE IF NOT EXISTS comment_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '评论点赞ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE (user_id, comment_id)
);

-- 创建收藏表
CREATE TABLE IF NOT EXISTS collections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content_id BIGINT NOT NULL COMMENT '内容ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE (user_id, content_id)
);

-- 创建通知表
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type INT NOT NULL COMMENT '通知类型：1-评论，2-点赞',
    related_id BIGINT NOT NULL COMMENT '关联ID（评论ID或内容ID）',
    related_user_id BIGINT COMMENT '关联用户ID',
    content VARCHAR(255) NOT NULL COMMENT '通知内容',
    status INT DEFAULT 0 COMMENT '状态：0-未读，1-已读',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
);

-- 创建索引
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);

-- 插入初始分类数据
INSERT INTO categories (name, description, sort_order) VALUES
('生活趣事', '分享日常生活中的有趣事情', 1),
('工作感悟', '分享工作中的心得体会', 2),
('情感故事', '分享情感经历和感悟', 3),
('学习笔记', '分享学习心得和笔记', 4),
('其他', '其他类型的分享', 5);

-- 插入初始标签数据
INSERT INTO tags (name) VALUES
('生活'),
('工作'),
('学习'),
('情感'),
('感悟'),
('成长'),
('分享'),
('经验'),
('技巧'),
('思考');

-- 插入测试用户数据（密码：123456）
INSERT INTO users (username, password, email, nickname) VALUES
('admin', '$2a$10$QH1M8pJ8M8pJ8M8pJ8M8O6lQ6lQ6lQ6lQ6lQ6lQ6lQ6lQ6lQ6', 'admin@example.com', '管理员'),
('test', '$2a$10$QH1M8pJ8M8pJ8M8pJ8M8O6lQ6lQ6lQ6lQ6lQ6lQ6lQ6lQ6', 'test@example.com', '测试用户');

-- 插入测试内容数据
INSERT INTO contents (user_id, title, content, category_id, tags) VALUES
(1, '欢迎使用生活分享网站', '这是一个测试内容，欢迎大家使用这个生活分享网站！在这里你可以分享生活中的趣事、工作感悟、情感故事等。', 1, '生活,分享'),
(2, '第一天工作的感受', '今天是我第一天上班，感觉很新鲜，学到了很多东西。公司的同事都很友好，期待接下来的工作。', 2, '工作,感悟'),
(1, '周末爬山记', '周末和朋友一起去爬山，天气很好，风景很美。爬山虽然累，但是很值得。', 1, '生活,感悟'),
(2, '学习笔记：Spring Boot', '学习了Spring Boot框架，感觉非常强大和便捷。推荐大家学习！', 4, '学习,技巧');
