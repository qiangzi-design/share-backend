@echo off
echo ===============================================
echo Life Sharing Website - Startup Script
echo ===============================================

echo.
echo [1/3] Initializing Database...
echo.

REM Check if MySQL is installed
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: MySQL is not installed or not added to environment variables
    echo Please install MySQL and add it to system PATH
    pause
    exit /b 1
)

REM Execute database initialization script
echo Creating database...
mysql -u root -p123456 < init_database.sql
if %errorlevel% neq 0 (
    echo Error: Database initialization failed. Please check MySQL connection
    echo Make sure MySQL service is running, username: root, password: 123456
    pause
    exit /b 1
)

echo Database initialized successfully!

echo.
echo [2/3] Starting Backend Service...
echo.

cd backend
echo Compiling backend service...
mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Error: Backend service compilation failed
    cd ..
    pause
    exit /b 1
)

echo Starting backend service...
start "Backend Service" mvn spring-boot:run

echo Backend service is starting, please wait...

echo.
echo [3/3] Starting Frontend Service...
echo.

cd ..\frontend
echo Starting frontend service...
start "Frontend Service" npm run dev

echo.
echo ===============================================
echo Startup completed!
echo ===============================================
echo Frontend URL: http://localhost:5173
echo Backend API URL: http://localhost:8080/api
echo ===============================================
echo Please wait for services to fully start before accessing
echo Press any key to exit...
pause >nul