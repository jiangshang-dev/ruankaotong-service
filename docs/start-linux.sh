#!/usr/bin/env bash

# ============================================================
#  软考通 AI 后端 — Ubuntu / Linux
#  用法（脚本和 jar 放同一目录，如 /home/node）：
#    chmod +x start-linux.sh
#    ./start-linux.sh start
#    tail -f ./ruankao-back.log
# ============================================================

export LLM_BASE_URL="${LLM_BASE_URL:-https://token-plan.cn-beijing.maas.aliyuncs.com/compatible-mode/v1}"
export LLM_MODEL_NAME="${LLM_MODEL_NAME:-qwen3.8-max}"
export LLM_API_KEY="${LLM_API_KEY:-请填写你的API_KEY}"
export LLM_VISION_MODEL_NAME="${LLM_VISION_MODEL_NAME:-}"
export LLM_TIMEOUT_SECONDS="${LLM_TIMEOUT_SECONDS:-180}"
export REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-}"
export REDIS_DATABASE="${REDIS_DATABASE:-0}"
export SERVER_PORT="${SERVER_PORT:-9001}"
JAVA_OPTS="${JAVA_OPTS:--Xms128m -Xmx512m -Dfile.encoding=UTF-8}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_NAME="ruankao-back"
APP_JAR=""
JAR_DIR=""
LOG_FILE=""
PID_FILE=""
JAVA_BIN=""

say() {
  echo "$*"
  if [[ -n "${LOG_FILE}" ]]; then
    echo "$(date '+%F %T') $*" >>"${LOG_FILE}"
  fi
}

find_jar() {
  if [[ -f "${SCRIPT_DIR}/ruankao-back-1.0-SNAPSHOT.jar" ]]; then
    echo "${SCRIPT_DIR}/ruankao-back-1.0-SNAPSHOT.jar"
    return 0
  fi
  local f
  for f in "${SCRIPT_DIR}"/ruankao-back*.jar; do
    if [[ -f "${f}" ]]; then
      echo "${f}"
      return 0
    fi
  done
  if [[ -f "${SCRIPT_DIR}/../target/ruankao-back-1.0-SNAPSHOT.jar" ]]; then
    echo "${SCRIPT_DIR}/../target/ruankao-back-1.0-SNAPSHOT.jar"
    return 0
  fi
  return 1
}

# 日志、pid 固定在 jar 所在目录
init_paths() {
  local jar
  jar="$(find_jar)" || {
    echo "未找到 ruankao-back-*.jar，请和脚本放在同一目录：${SCRIPT_DIR}"
    exit 1
  }
  JAR_DIR="$(cd "$(dirname "${jar}")" && pwd)"
  APP_JAR="${JAR_DIR}/$(basename "${jar}")"
  LOG_FILE="${JAR_DIR}/ruankao-back.log"
  PID_FILE="${JAR_DIR}/ruankao-back.pid"
  touch "${LOG_FILE}"
}

find_java() {
  local p
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    echo "${JAVA_HOME}/bin/java"
    return 0
  fi
  for p in \
    /home/jdk21/bin/java \
    /home/jdk21/jdk-21/bin/java \
    /usr/lib/jvm/java-21-openjdk-amd64/bin/java \
    /usr/lib/jvm/java-21-openjdk/bin/java
  do
    if [[ -x "${p}" ]]; then
      echo "${p}"
      return 0
    fi
  done
  if [[ -d /home/jdk21 ]]; then
    p="$(find /home/jdk21 -name java -type f 2>/dev/null | grep '/bin/java$' | head -n 1)"
    if [[ -n "${p}" && -x "${p}" ]]; then
      echo "${p}"
      return 0
    fi
  fi
  command -v java 2>/dev/null
}

running_pid() {
  if [[ ! -f "${PID_FILE}" ]]; then
    return 1
  fi
  local pid
  pid="$(tr -d ' \t\r\n' <"${PID_FILE}")"
  if [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null; then
    echo "${pid}"
    return 0
  fi
  rm -f "${PID_FILE}"
  return 1
}

hint_log() {
  echo
  echo "查看日志："
  echo "  tail -f ${LOG_FILE}"
  echo "  tail -f ./ruankao-back.log"
}

cmd_start() {
  init_paths
  say "正在启动 ${APP_NAME} ..."
  say "jar目录: ${JAR_DIR}"
  say "jar: ${APP_JAR}"
  say "日志: ${LOG_FILE}"

  local pid
  pid="$(running_pid || true)"
  if [[ -n "${pid}" ]]; then
    say "已在运行，pid=${pid}"
    hint_log
    exit 0
  fi

  JAVA_BIN="$(find_java || true)"
  if [[ -z "${JAVA_BIN}" ]]; then
    say "错误：未找到 java。请执行：export JAVA_HOME=/home/jdk21"
    say "或：sudo apt install -y openjdk-21-jdk"
    hint_log
    exit 1
  fi
  say "java: ${JAVA_BIN}"
  "${JAVA_BIN}" -version >>"${LOG_FILE}" 2>&1 || true
  "${JAVA_BIN}" -version 2>&1 | head -n 1

  if [[ "${LLM_API_KEY}" == "请填写你的API_KEY" || -z "${LLM_API_KEY}" ]]; then
    say "错误：请先 export LLM_API_KEY=sk-你的密钥  或编辑脚本填写 LLM_API_KEY"
    hint_log
    exit 1
  fi

  cd "${JAR_DIR}" || exit 1
  say "执行: ${JAVA_BIN} ${JAVA_OPTS} -jar ${APP_JAR}"
  nohup "${JAVA_BIN}" ${JAVA_OPTS} -jar "${APP_JAR}" >>"${LOG_FILE}" 2>&1 &
  pid=$!
  echo "${pid}" >"${PID_FILE}"
  sleep 2

  if kill -0 "${pid}" 2>/dev/null; then
    say "启动成功 pid=${pid}  端口=${SERVER_PORT}"
    say "健康检查: http://127.0.0.1:${SERVER_PORT}/api/ai/essay/health"
    hint_log
  else
    say "启动失败，进程已退出。最近日志："
    echo "-------- ${LOG_FILE} --------"
    tail -n 80 "${LOG_FILE}" || true
    rm -f "${PID_FILE}"
    exit 1
  fi
}

cmd_stop() {
  init_paths
  local pid
  pid="$(running_pid || true)"
  if [[ -z "${pid}" ]]; then
    say "${APP_NAME} 未运行"
    exit 0
  fi
  say "正在停止 pid=${pid}"
  kill "${pid}" 2>/dev/null || true
  local i
  for i in $(seq 1 15); do
    if ! kill -0 "${pid}" 2>/dev/null; then
      rm -f "${PID_FILE}"
      say "已停止"
      exit 0
    fi
    sleep 1
  done
  kill -9 "${pid}" 2>/dev/null || true
  rm -f "${PID_FILE}"
  say "已强制停止"
}

cmd_status() {
  init_paths
  local pid
  pid="$(running_pid || true)"
  if [[ -n "${pid}" ]]; then
    say "运行中 pid=${pid}  端口=${SERVER_PORT}"
    hint_log
  else
    say "未运行"
    echo "日志: ${LOG_FILE}"
    hint_log
    exit 1
  fi
}

cmd_run() {
  init_paths
  JAVA_BIN="$(find_java || true)"
  if [[ -z "${JAVA_BIN}" ]]; then
    echo "未找到 java"
    exit 1
  fi
  if [[ "${LLM_API_KEY}" == "请填写你的API_KEY" || -z "${LLM_API_KEY}" ]]; then
    echo "请先 export LLM_API_KEY=sk-..."
    exit 1
  fi
  echo "前台运行，日志同时写入 ${LOG_FILE}"
  cd "${JAR_DIR}" || exit 1
  "${JAVA_BIN}" ${JAVA_OPTS} -jar "${APP_JAR}" 2>&1 | tee -a "${LOG_FILE}"
}

CMD="${1:-start}"
case "${CMD}" in
  start) cmd_start ;;
  stop) cmd_stop ;;
  restart) cmd_stop; cmd_start ;;
  status) cmd_status ;;
  run) cmd_run ;;
  *)
    echo "用法: $0 {start|stop|restart|status|run}"
    exit 1
    ;;
esac
