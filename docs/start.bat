@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul
title 软考通 AI 后端

REM ============================================================
REM  模型配置（OpenAI 兼容接口）
REM ============================================================
set "LLM_BASE_URL=https://token-plan.cn-beijing.maas.aliyuncs.com/compatible-mode/v1"
set "LLM_MODEL_NAME=qwen3.8-max"
set "LLM_API_KEY=请填写你的API_KEY"
set "LLM_VISION_MODEL_NAME="
set "LLM_TIMEOUT_SECONDS=180"

REM ============================================================
REM  Redis 配置（AgentScope 会话 / 论文指导历史）
REM ============================================================
set "REDIS_HOST=127.0.0.1"
set "REDIS_PORT=6379"
set "REDIS_PASSWORD="
set "REDIS_DATABASE=0"

REM ============================================================
REM  服务端口
REM ============================================================
set "SERVER_PORT=9001"

cd /d "%~dp0.."

set "APP_JAR=%cd%\target\ruankao-back-1.0-SNAPSHOT.jar"
if exist "%~dp0ruankao-back-1.0-SNAPSHOT.jar" (
  set "APP_JAR=%~dp0ruankao-back-1.0-SNAPSHOT.jar"
)

where java >nul 2>&1
if errorlevel 1 (
  echo 未找到 java，请先安装 JDK 21 并加入 PATH。
  pause
  exit /b 1
)

if "%LLM_API_KEY%"=="请填写你的API_KEY" (
  echo 请先编辑 docs\start.bat，填写 LLM_API_KEY。
  pause
  exit /b 1
)

if not exist "%APP_JAR%" (
  echo 未找到 jar：%APP_JAR%
  where mvn >nul 2>&1
  if errorlevel 1 (
    echo 请先在仓库根目录执行：mvn -DskipTests package
    pause
    exit /b 1
  )
  echo 正在打包...
  call mvn -DskipTests package
  if errorlevel 1 (
    echo 打包失败。
    pause
    exit /b 1
  )
)

echo.
echo 模型: %LLM_MODEL_NAME%
echo 接口: %LLM_BASE_URL%
echo Redis: %REDIS_HOST%:%REDIS_PORT%  db=%REDIS_DATABASE%
echo 端口: %SERVER_PORT%
echo jar:  %APP_JAR%
echo.

java -jar "%APP_JAR%"
set "EXIT_CODE=%ERRORLEVEL%"
echo.
echo 进程已退出，代码 %EXIT_CODE%。
pause
exit /b %EXIT_CODE%
