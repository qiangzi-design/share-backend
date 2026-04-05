@echo off
chcp 65001 >nul
echo ===============================================
echo 生活分享网站 - 启动脚本
echo ===============================================

echo.
echo [1/3] 初始化数据库...
echo.

REM 检查MySQL是否安装
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: MySQL未安装或未添加到环境变量
    echo 请先安装MySQL并将其添加到系统PATH中
    pause
    exit /b 1
)

REM 执行数据库初始化脚本
echo 正在创建数据库...
mysql -u root -p123456 < init_database.sql
if %errorlevel% neq 0 (
    echo 错误: 数据库初始化失败，请检查MySQL连接信息
    echo 请确保MySQL服务正在运行，用户名root，密码123456
    pause
    exit /b 1
)

echo 数据库初始化成功！

echo.
echo [2/3] 启动后端服务...
echo.

cd backend
echo 正在编译后端服务...
mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo 错误: 后端服务编译失败
    cd ..
    pause
    exit /b 1
)

echo 正在启动后端服务...
start "后端服务" mvn spring-boot:run

echo 后端服务启动中，请稍候...

echo.
echo [3/3] 启动前端服务...
echo.

cd ..\frontend
echo 正在启动前端服务...
start "前端服务" npm run dev

echo.
echo ===============================================
echo 启动完成！
echo ===============================================
echo 前端访问地址: http://localhost:5173
echo 后端API地址: http://localhost:8080/api
echo ===============================================
echo 请等待服务完全启动后再访问
echo 按任意键退出...
pause >nul