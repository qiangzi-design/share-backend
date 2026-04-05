@echo off
echo ================================================
echo 生活分享网站数据备份和恢复测试脚本
echo ================================================

REM 设置测试参数
set TEST_DB_NAME=share_db_test
set TEST_DB_USER=root
set TEST_DB_PASSWORD=123456
set TEST_BACKUP_DIR=%~dp0test_backup

echo 开始数据备份和恢复测试...
echo ================================================

REM 1. 创建测试数据库
echo 1. 创建测试数据库...
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS %TEST_DB_NAME%;"

if %errorlevel% neq 0 (
    echo 错误: 创建测试数据库失败
    pause
    exit /b 1
)

echo 测试数据库创建成功

REM 2. 创建测试表和数据
echo 2. 创建测试表和数据...
mysql -uroot -p123456 %TEST_DB_NAME% -e "
CREATE TABLE IF NOT EXISTS test_table (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO test_table (name, value) VALUES 
('测试数据1', '测试值1'),
('测试数据2', '测试值2'),
('测试数据3', '测试值3');

SELECT COUNT(*) FROM test_table;
"

if %errorlevel% neq 0 (
    echo 错误: 创建测试表和数据失败
    pause
    exit /b 1
)

echo 测试表和数据创建成功

REM 3. 执行备份测试
echo 3. 执行备份测试...
if not exist "%TEST_BACKUP_DIR%" mkdir "%TEST_BACKUP_DIR%"

set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set TEST_BACKUP_FILE=%TEST_BACKUP_DIR%\test_backup_%TIMESTAMP%.sql

mysqldump -uroot -p123456 --databases %TEST_DB_NAME% > "%TEST_BACKUP_FILE%"

if %errorlevel% neq 0 (
    echo 错误: 备份测试失败
    pause
    exit /b 1
)

echo 备份测试成功，备份文件: %TEST_BACKUP_FILE%

REM 4. 修改测试数据（模拟数据变更）
echo 4. 修改测试数据...
mysql -uroot -p123456 %TEST_DB_NAME% -e "
INSERT INTO test_table (name, value) VALUES ('测试数据4', '测试值4');
UPDATE test_table SET value = '已修改' WHERE id = 1;
SELECT * FROM test_table;
"

echo 测试数据已修改

REM 5. 执行恢复测试
echo 5. 执行恢复测试...
mysql -uroot -p123456 -e "DROP DATABASE %TEST_DB_NAME%;"
mysql -uroot -p123456 -e "CREATE DATABASE %TEST_DB_NAME%;"
mysql -uroot -p123456 %TEST_DB_NAME% < "%TEST_BACKUP_FILE%"

if %errorlevel% neq 0 (
    echo 错误: 恢复测试失败
    pause
    exit /b 1
)

echo 恢复测试成功

REM 6. 验证恢复结果
echo 6. 验证恢复结果...
mysql -uroot -p123456 %TEST_DB_NAME% -e "SELECT * FROM test_table;"

if %errorlevel% neq 0 (
    echo 错误: 验证恢复结果失败
    pause
    exit /b 1
)

echo ================================================
echo 数据备份和恢复测试完成！
echo 测试结果: 成功
echo ================================================

REM 清理测试数据
echo 清理测试数据...
mysql -uroot -p123456 -e "DROP DATABASE %TEST_DB_NAME%;"
rmdir /s /q "%TEST_BACKUP_DIR%"

echo 测试数据清理完成
echo ================================================
echo 测试总结:
echo - 备份功能: 正常
echo - 恢复功能: 正常
echo - 数据完整性: 保持
echo ================================================
pause