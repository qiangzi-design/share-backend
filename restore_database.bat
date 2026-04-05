@echo off
echo ================================================
echo 生活分享网站数据库恢复脚本
echo ================================================

REM 设置数据库参数
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=share_db
set DB_USER=root
set DB_PASSWORD=123456

REM 设置备份目录
set BACKUP_DIR=%~dp0backup

echo 数据库恢复操作
echo 数据库: %DB_NAME%
echo 备份目录: %BACKUP_DIR%
echo ================================================

REM 检查备份目录是否存在
if not exist "%BACKUP_DIR%" (
    echo 错误: 备份目录不存在
    pause
    exit /b 1
)

REM 列出可用的备份文件
echo 可用的备份文件:
echo ================================================
dir "%BACKUP_DIR%\*.zip" /B /O-D
echo ================================================

REM 提示用户选择备份文件
set /p BACKUP_FILE=请输入要恢复的备份文件名（包含.zip后缀）:

REM 检查文件是否存在
if not exist "%BACKUP_DIR%\%BACKUP_FILE%" (
    echo 错误: 指定的备份文件不存在
    pause
    exit /b 1
)

echo ================================================
echo 准备恢复数据库...
echo 备份文件: %BACKUP_DIR%\%BACKUP_FILE%
echo ================================================

REM 解压备份文件
echo 解压备份文件...
set TEMP_DIR=%BACKUP_DIR%\temp_restore
if exist "%TEMP_DIR%" rmdir /s /q "%TEMP_DIR%"
mkdir "%TEMP_DIR%"

powershell -command "Expand-Archive -Path '%BACKUP_DIR%\%BACKUP_FILE%' -DestinationPath '%TEMP_DIR%' -Force"

if %errorlevel% neq 0 (
    echo 错误: 备份文件解压失败
    pause
    exit /b 1
)

REM 查找SQL文件
for /f "delims=" %%i in ('dir "%TEMP_DIR%\*.sql" /B') do set SQL_FILE=%%i

if not defined SQL_FILE (
    echo 错误: 在解压目录中未找到SQL文件
    pause
    exit /b 1
)

echo 找到SQL文件: %SQL_FILE%

REM 停止应用服务（如果正在运行）
echo 停止应用服务...
taskkill /f /im java.exe 2>nul

REM 恢复数据库
echo ================================================
echo 开始恢复数据库...
mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASSWORD% %DB_NAME% < "%TEMP_DIR%\%SQL_FILE%"

if %errorlevel% equ 0 (
    echo 数据库恢复成功！
) else (
    echo 错误: 数据库恢复失败
    pause
    exit /b 1
)

REM 清理临时文件
echo 清理临时文件...
rmdir /s /q "%TEMP_DIR%"

echo ================================================
echo 数据库恢复任务完成！
echo 已从备份文件: %BACKUP_FILE% 恢复数据
echo ================================================
echo 提示: 请重新启动应用服务以应用更改
pause