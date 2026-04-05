@echo off
echo ================================================
echo 生活分享网站生产环境部署脚本
echo ================================================

REM 设置环境变量
set JAVA_HOME=C:\Program Files\Java\jdk11
set NODE_HOME=C:\Program Files\nodejs
set PATH=%NODE_HOME%\node_modules\npm\bin;%NODE_HOME%;%JAVA_HOME%\bin;%PATH%

echo 环境检查:
echo JAVA_HOME: %JAVA_HOME%
echo NODE_HOME: %NODE_HOME%
echo ================================================

REM 检查必要工具
echo 检查必要工具...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: Java未安装或JAVA_HOME未正确配置
    pause
    exit /b 1
)

node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: Node.js未安装或NODE_HOME未正确配置
    pause
    exit /b 1
)

npm -v >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: npm未正确配置
    pause
    exit /b 1
)

echo 工具检查通过
echo ================================================

REM 1. 前端构建
echo 开始前端构建...
cd frontend
echo 安装前端依赖...
npm install
if %errorlevel% neq 0 (
    echo 错误: 前端依赖安装失败
    pause
    exit /b 1
)

echo 构建前端项目...
npm run build
if %errorlevel% neq 0 (
    echo 错误: 前端构建失败
    pause
    exit /b 1
)

echo 前端构建完成
cd ..
echo ================================================

REM 2. 后端打包
echo 开始后端打包...
cd backend
echo 清理并打包后端项目...
mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo 错误: 后端打包失败
    pause
    exit /b 1
)

echo 后端打包完成
cd ..
echo ================================================

REM 3. 数据库设置
echo 数据库设置...
echo 请确保MySQL服务已启动且可以访问
echo 数据库配置信息:
echo - 数据库名称: share_db
echo - 用户名: root
echo - 密码: 123456
echo ================================================

REM 4. 部署应用
echo 部署应用...
echo 停止现有服务（如果存在）...
taskkill /f /im java.exe 2>nul

echo 设置生产环境变量...
set SPRING_PROFILES_ACTIVE=prod
set SERVER_PORT=8080

echo 设置JVM参数...
set JAVA_OPTS=-Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1HeapRegionSize=4m -XX:InitiatingHeapOccupancyPercent=70 -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log -XX:+HeapDumpOnOutOfMemoryError

echo 启动应用...
cd backend\target
for /f "delims=" %%i in ('dir /b share-*-SNAPSHOT.jar') do set JAR_FILE=%%i

echo 正在启动: %JAR_FILE%
java %JAVA_OPTS% -jar %JAR_FILE%

echo ================================================
echo 应用启动完成！
echo 访问地址: http://localhost:8080/api
echo 前端地址: http://localhost:8080
echo ================================================