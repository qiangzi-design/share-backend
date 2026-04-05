# 生活分享网站

## 项目简介

生活分享网站是一个前后端分离的Web应用，用户可以在平台上分享生活点滴、美食、旅行、心情等内容。

## 技术栈

### 前端
- **Vue 3**: 渐进式JavaScript框架
- **Element Plus**: UI组件库
- **Vue Router**: 路由管理
- **Axios**: HTTP客户端

### 后端
- **Spring Boot 3.x**: Java开发框架
- **MyBatis Plus**: ORM框架
- **MySQL**: 关系型数据库
- **Redis**: 缓存数据库

## 项目结构

```
share/
├── frontend/          # 前端项目
│   ├── src/
│   │   ├── components/   # 组件
│   │   ├── views/        # 视图
│   │   ├── router/       # 路由
│   │   ├── api/          # API请求
│   │   └── utils/        # 工具函数
│   ├── public/        # 静态资源
│   ├── package.json   # 依赖配置
│   └── vite.config.js # Vite配置
├── backend/           # 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/share/
│   │   │   │   ├── config/      # 配置类
│   │   │   │   ├── entity/      # 实体类
│   │   │   │   ├── mapper/      # Mapper接口
│   │   │   │   ├── service/     # 服务层
│   │   │   │   └── controller/  # 控制器
│   │   │   └── resources/
│   │   │       └── application.yml # 配置文件
│   │   └── test/      # 测试代码
│   ├── pom.xml        # Maven配置
│   └── README.md      # 后端说明
├── .gitignore         # Git忽略文件
└── README.md          # 项目说明
```

## 开发环境要求

- **Node.js**: 24.x+
- **npm**: 8.x+
- **JDK**: 17+
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **Redis**: 6.0+

## 安装和运行

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 http://localhost:5173

### 后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端默认运行在 http://localhost:8080/api

## 数据库配置

1. 创建MySQL数据库：`share_db`
2. 修改`backend/src/main/resources/application.yml`中的数据库配置：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/share_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
       username: your_username
       password: your_password
   ```

## Redis配置

修改`backend/src/main/resources/application.yml`中的Redis配置：
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: your_password
    database: 0
```

## 功能特性

- 用户注册、登录（JWT）
- 匿名浏览内容列表/详情，登录后可发布、点赞、评论
- 内容搜索、分类筛选、分页浏览
- 内容编辑与逻辑删除（仅作者）
- 个人资料管理（昵称/简介/头像）
- 个人发布内容管理

## 接口路径说明

- 新规范路径：`/api/*`
- 兼容路径：`/api/api/*`（仅保留一个版本，响应头会带弃用提示）

## 数据库迁移（手工）

- 迁移脚本：`database_migration_phase1_phase2.sql`
- 回滚脚本：`database_migration_phase1_phase2_rollback.sql`
- 当前不使用 Flyway 自动迁移

## 开发计划

- [ ] 用户认证系统
- [ ] 分享内容管理
- [ ] 评论互动功能
- [ ] 文件上传功能
- [ ] 搜索功能
- [ ] 移动端适配

## 许可证

MIT License
