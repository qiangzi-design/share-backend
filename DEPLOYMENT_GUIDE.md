# 生活分享网站部署指南

## 项目概述

生活分享网站是一个基于Vue 3和Spring Boot的现代化Web应用，提供用户分享生活内容、评论互动等功能。

### 技术栈

- **前端**: Vue 3 + Element Plus + Vite
- **后端**: Spring Boot 2.x + MyBatis Plus
- **数据库**: MySQL 8.0+
- **CI/CD**: GitHub Actions

## 环境要求

### 系统要求

- Windows Server 2016/2019/2022 或 Linux系统
- Java JDK 11+
- Node.js 18+
- MySQL 8.0+
- Maven 3.6+

### 硬件要求

- CPU: 4核及以上
- 内存: 8GB及以上
- 存储: 100GB及以上

## 部署步骤

### 1. 环境准备

#### 安装必要软件

```bash
# Windows系统
# 安装Java JDK 11
# 安装Node.js 18
# 安装MySQL 8.0
# 安装Maven 3.6+

# Linux系统 (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-11-jdk nodejs npm mysql-server maven
```

#### 配置环境变量

```bash
# Windows系统
set JAVA_HOME=C:\Program Files\Java\jdk-11
set NODE_HOME=C:\Program Files\nodejs
set PATH=%NODE_HOME%;%JAVA_HOME%\bin;%PATH%

# Linux系统
echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64' >> ~/.bashrc
echo 'export NODE_HOME=/usr/bin/node' >> ~/.bashrc
echo 'export PATH=$NODE_HOME:$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### 2. 数据库配置

#### 创建数据库

```sql
CREATE DATABASE share_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'share_user'@'localhost' IDENTIFIED BY 'Share@123456';
GRANT ALL PRIVILEGES ON share_db.* TO 'share_user'@'localhost';
FLUSH PRIVILEGES;
```

#### 导入数据库索引

```bash
mysql -u root -p share_db < database_indexes.sql
```

### 3. 应用部署

#### 使用部署脚本（推荐）

```bash
# 在项目根目录执行
.\deploy_prod.bat
```

部署脚本会自动执行以下步骤：
1. 检查环境依赖
2. 安装前端依赖并构建
3. 打包后端应用
4. 启动应用服务

#### 手动部署步骤

1. **前端构建**

```bash
cd frontend
npm install
npm run build
```

2. **后端打包**

```bash
cd backend
mvn clean package -DskipTests
```

3. **启动应用**

```bash
# 设置环境变量
set SPRING_PROFILES_ACTIVE=prod
set SERVER_PORT=8080

# 设置JVM参数
set JAVA_OPTS=-Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

# 启动应用
cd backend\target
java %JAVA_OPTS% -jar share-*-SNAPSHOT.jar
```

### 4. 访问应用

- **前端地址**: http://localhost:8080
- **后端API**: http://localhost:8080/api
- **默认管理员账号**: admin / admin123

## CI/CD配置

### GitHub Actions配置

项目已配置GitHub Actions自动构建和部署流程：

1. **触发条件**:
   - 推送到main/master分支时自动构建测试
   - 合并到main/master分支时自动部署

2. **配置步骤**:
   - 在GitHub仓库Settings -> Secrets and variables -> Actions中添加以下secrets:
     - DEPLOY_SERVER: 部署服务器地址
     - DEPLOY_USER: 服务器用户名
     - DEPLOY_KEY: SSH私钥
     - DEPLOY_PATH: 部署路径

3. **工作流程**:
   - 构建测试: 自动构建前端和后端，运行测试
   - 部署: 将构建产物部署到生产服务器

## 数据备份和恢复

### 自动备份

```bash
# 执行数据库备份
.\backup_database.bat
```

备份脚本功能：
- 自动创建带时间戳的备份文件
- 压缩备份文件以节省空间
- 自动清理7天前的旧备份

### 数据恢复

```bash
# 执行数据库恢复
.\restore_database.bat
```

恢复步骤：
1. 脚本会列出所有可用的备份文件
2. 输入要恢复的备份文件名
3. 脚本自动解压并恢复数据库

### 备份测试

```bash
# 运行备份恢复测试
.\test_backup_restore.bat
```

测试脚本会创建测试数据库，验证备份和恢复功能的正确性。

## 运维管理

### 服务管理

```bash
# 启动服务
.\start_prod.bat

# 停止服务
taskkill /f /im java.exe

# 查看服务状态
netstat -an | findstr :8080
```

### 日志管理

- **应用日志**: 应用启动目录下的`logs`文件夹
- **GC日志**: 应用启动目录下的`gc.log`文件
- **错误日志**: 应用控制台输出

### 性能监控

- **JVM监控**: 使用JConsole或VisualVM监控JVM状态
- **数据库监控**: 使用MySQL Workbench监控数据库性能
- **应用监控**: 集成Prometheus和Grafana进行监控

## 故障排查

### 常见问题

1. **端口被占用**
   ```bash
   netstat -ano | findstr :8080
   taskkill /f /pid [进程ID]
   ```

2. **数据库连接失败**
   - 检查MySQL服务是否启动
   - 检查数据库配置是否正确
   - 检查数据库用户权限

3. **前端无法访问后端API**
   - 检查CORS配置
   - 检查后端服务是否正常运行
   - 检查网络防火墙设置

4. **应用启动失败**
   - 查看应用日志
   - 检查JVM内存配置
   - 检查数据库连接

### 紧急恢复流程

1. **停止当前服务**
2. **从最近的备份恢复数据库**
3. **重新启动应用服务**
4. **验证服务正常运行**

## 更新维护

### 应用更新

1. **获取最新代码**
   ```bash
   git pull origin main
   ```

2. **重新部署**
   ```bash
   .\deploy_prod.bat
   ```

3. **验证更新**
   - 检查应用版本
   - 测试核心功能
   - 监控系统性能

### 数据库更新

1. **备份当前数据库**
2. **执行数据库迁移脚本**
3. **验证数据完整性**

## 安全注意事项

1. **密码管理**
   - 定期更换数据库密码
   - 使用强密码策略
   - 避免在代码中硬编码密码

2. **访问控制**
   - 限制服务器访问权限
   - 配置防火墙规则
   - 使用HTTPS加密传输

3. **数据保护**
   - 定期备份数据库
   - 加密敏感数据
   - 实施数据访问审计

## 联系支持

如有部署问题，请联系技术支持团队：
- 技术支持邮箱: support@example.com
- 紧急联系电话: 400-123-4567

---

**文档版本**: 1.0  
**最后更新**: 2024-01-15  
**维护团队**: 生活分享网站技术组