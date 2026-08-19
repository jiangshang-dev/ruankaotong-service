#!/usr/bin/env bash
set -euo pipefail

# ============================================================
#  模型配置（OpenAI 兼容接口）
# ============================================================
export LLM_BASE_URL="${LLM_BASE_URL:-https://token-plan.cn-beijing.maas.aliyuncs.com/compatible-mode/v1}"
export LLM_MODEL_NAME="${LLM_MODEL_NAME:-qwen3.8-max}"
export LLM_API_KEY="${LLM_API_KEY:-请填写你的API_KEY}"
export LLM_VISION_MODEL_NAME="${LLM_VISION_MODEL_NAME:-}"
export LLM_TIMEOUT_SECONDS="${LLM_TIMEOUT_SECONDS:-180}"

# ============================================================
#  Redis 配置（AgentScope 会话 / 论文指导与案例讲解历史）
# ============================================================
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-}"
export REDIS_DATABASE="${REDIS_DATABASE:-0}"

# ============================================================
#  服务端口
# ============================================================
export SERVER_PORT="${SERVER_PORT:-9001}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

APP_JAR="${ROOT_DIR}/target/ruankao-back-1.0-SNAPSHOT.jar"
if [[ -f "${SCRIPT_DIR}/ruankao-back-1.0-SNAPSHOT.jar" ]]; then
  APP_JAR="${SCRIPT_DIR}/ruankao-back-1.0-SNAPSHOT.jar"
fi

if ! command -v java >/dev/null 2>&1; then
  echo "未找到 java，请先安装 JDK 21 并加入 PATH。"
  echo "可用：brew install openjdk@21"
  exit 1
fi

if [[ "${LLM_API_KEY}" == "请填写你的API_KEY" || -z "${LLM_API_KEY}" ]]; then
  echo "请先编辑 docs/start.sh，填写 LLM_API_KEY；或启动前 export LLM_API_KEY=sk-..."
  exit 1
fi

if [[ ! -f "${APP_JAR}" ]]; then
  echo "未找到 jar：${APP_JAR}"
  if ! command -v mvn >/dev/null 2>&1; then
    echo "请先在仓库根目录执行：mvn -DskipTests package"
    exit 1
  fi
  echo "正在打包..."
  mvn -DskipTests package
fi

echo
echo "模型: ${LLM_MODEL_NAME}"
echo "接口: ${LLM_BASE_URL}"
echo "Redis: ${REDIS_HOST}:${REDIS_PORT}  db=${REDIS_DATABASE}"
echo "端口: ${SERVER_PORT}"
echo "jar:  ${APP_JAR}"
echo

exec java -jar "${APP_JAR}"
