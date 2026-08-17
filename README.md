# 软考通 AI 后端（AgentScope 2）

无登录、无数据库、无会话记忆。每次润色/评分/案例分析新建 `ReActAgent`，单轮调用即结束。

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

### 案例分析识图解答

`POST /api/ai/case/solve`

```json
{
  "subject": "系统架构设计师",
  "title": "2024 上半年试题一",
  "topicText": "",
  "images": [
    { "mimeType": "image/png", "base64": "..." }
  ]
}
```

### 案例分析评分

`POST /api/ai/case/score`

```json
{
  "subject": "系统架构设计师",
  "title": "2024 上半年试题一",
  "topicText": "",
  "answerText": "【问题1】\n……",
  "images": [
    { "mimeType": "image/png", "base64": "..." }
  ]
}
```

识图模型可用环境变量 `LLM_VISION_MODEL_NAME` 覆盖；留空则使用 `LLM_MODEL_NAME`。

## 系统提示词

- `src/main/resources/prompts/ruankao-polish-system.txt`
- `src/main/resources/prompts/ruankao-score-system.txt`
- `src/main/resources/prompts/ruankao-case-solve-system.txt`
- `src/main/resources/prompts/ruankao-case-score-system.txt`
