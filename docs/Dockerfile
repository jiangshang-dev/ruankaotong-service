# 软考通 AI 后端
# 先在本机打好包：mvn -DskipTests package
#
# 仓库根目录构建：
#   docker build -f docs/Dockerfile -t ruankao-back:1.0 .
#
# 服务器上 jar 和 Dockerfile 在同一目录（如 /home/node）时：
#   docker build --build-arg JAR_FILE=ruankao-back-1.0-SNAPSHOT.jar -t ruankao-back:1.0 .
#
# 运行（Linux 上 Redis 已在本机 6379 时用 host 网络）：
#   docker run -d --name ruankao-back --restart unless-stopped --network host \
#     -e LLM_API_KEY=sk-你的密钥 \
#     -e SERVER_PORT=9001 \
#     -e REDIS_HOST=127.0.0.1 \
#     ruankao-back:1.0
#
# 不要把真实 API Key 写进本文件。

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ARG JAR_FILE=target/ruankao-back-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

ENV TZ=Asia/Shanghai \
    SERVER_PORT=9001 \
    REDIS_HOST=127.0.0.1 \
    REDIS_PORT=6379 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxRAMPercentage=50"

EXPOSE 9001

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
