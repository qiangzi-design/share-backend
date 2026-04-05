@echo off
echo ================================================
echo 生活分享网站数据库备份脚本
echo ================================================

REM 设置数据库参数
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=share_db
set DB_USER=root
set DB_PASSWORD=123456

REM 设置备份目录
set BACKUP_DIR=%~dp0backup
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

REM 设置备份文件名（包含时间戳）
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set BACKUP_FILE=%BACKUP_DIR%\share_db_backup_%TIMESTAMP%.sql

echo 开始数据库备份...
echo 数据库: %DB_NAME%
echo 备份文件: %BACKUP_FILE%
echo ================================================

REM 执行备份
mysqldump -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASSWORD% --databases %DB_NAME% --single-transaction --quick --lock-tables=false > "%BACKUP_FILE%"

if %errorlevel% equ 0 (
    echo 数据库备份成功！
    echo 备份文件大小:
    dir "%BACKUP_FILE%"
) else (
    echo 错误: 数据库备份失败
    pause
    exit /b 1
)

REM 压缩备份文件
echo 压缩备份文件...
powershell -command "Compress-Archive -Path '%BACKUP_FILE%' -DestinationPath '%BACKUP_FILE%.zip' -Force"

if %errorlevel% equ 0 (
    echo 备份文件压缩成功！
    REM 删除未压缩的SQL文件
    del "%BACKUP_FILE%"
    echo 已删除临时SQL文件
) else (
    echo 警告: 备份文件压缩失败
)

REM 清理旧备份（保留最近7天的备份）
echo ================================================
echo 清理旧备份文件...
forfiles /p "%BACKUP_DIR%" /s /m "*.zip" /d -7 /c "cmd /c del @path"
echo 旧备份清理完成

echo ================================================
echo 数据库备份任务完成！
echo 备份文件: %BACKUP_FILE%.zip
echo ================================================
pause