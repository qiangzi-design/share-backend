@echo off
echo 启动生活分享网站生产环境
echo ======================================

REM 设置JVM参数
set JAVA_OPTS=-Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1HeapRegionSize=4m -XX:InitiatingHeapOccupancyPercent=70 -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log -XX:+HeapDumpOnOutOfMemoryError

REM 设置环境变量
set SPRING_PROFILES_ACTIVE=prod
set SERVER_PORT=8080

echo JVM参数: %JAVA_OPTS%
echo 环境配置: %SPRING_PROFILES_ACTIVE%
echo 服务端口: %SERVER_PORT%
echo ======================================

REM 启动应用
echo 正在启动应用...
cd backend
mvn spring-boot:run -Dspring-boot.run.jvmArguments="%JAVA_OPTS%" -Dspring-boot.run.profiles=%SPRING_PROFILES_ACTIVE%