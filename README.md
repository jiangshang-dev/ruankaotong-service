# 软考通 AI 后端（AgentScope 2）

无登录、无数据库、无会话记忆。每次润色/评分新建 `ReActAgent`，单轮调用即结束。

## 启动

```bash
cd /Users/xiaobai/Deveploer/workspace/lingxi/ruankao-back
# 可按环境覆盖模型
export LLM_BASE_URL=http://192.168.1.62:8000/v1
export LLM_MODEL_NAME=/data/model/Qwen3.5-35B-A3B-GPTQ-Int4
export LLM_API_KEY=sk-local
mvn spring-boot:run
```

默认端口：`9001`

## 接口

### 润色论文

`POST /api/ai/essay/polish`

```json
{
  "subject": "系统架构设计师",
  "topic": "论大模型智能运维……",
  "part": "abstract|body|all",
  "abstractText": "……",
  "bodyText": "……"
}
```

### AI 评分

`POST /api/ai/essay/score`

```json
{
  "subject": "系统架构设计师",
  "topic": "……",
  "abstractText": "……",
  "bodyText": "……"
}
```

## 系统提示词

- `src/main/resources/prompts/ruankao-polish-system.txt`
- `src/main/resources/prompts/ruankao-score-system.txt`
