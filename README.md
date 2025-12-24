# BCNL Database QA API

基于 FastAPI 的数据库问答系统，支持流式输出。

## 功能特性

- 🔍 自然语言查询数据库
- 💬 流式输出回答
- 🚀 FastAPI 高性能接口
- 📊 自动生成和执行 SQL 查询

## 安装依赖

```bash
pip install -r requirements.txt
```

## 启动服务

```bash
python app.py
```

或者使用 uvicorn：

```bash
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

服务启动后，访问 http://localhost:8000/docs 查看 API 文档。

## API 接口

### 1. 流式查询接口（推荐）

**POST** `/query/stream`

请求体：
```json
{
  "query": "有哪些组织"
}
```

响应：Server-Sent Events (SSE) 流式输出

### 2. 普通查询接口

**POST** `/query`

请求体：
```json
{
  "query": "有哪些组织"
}
```

响应：
```json
{
  "query": "有哪些组织",
  "response": "根据数据库查询结果..."
}
```

### 3. 健康检查

**GET** `/health`

响应：
```json
{
  "status": "healthy"
}
```

## 测试客户端

使用提供的测试客户端：

```bash
python test_client.py "有哪些组织"
```

或者直接使用 curl：

```bash
# 流式查询
curl -X POST "http://localhost:8000/query/stream" \
  -H "Content-Type: application/json" \
  -d '{"query": "有哪些组织"}' \
  --no-buffer

# 普通查询
curl -X POST "http://localhost:8000/query" \
  -H "Content-Type: application/json" \
  -d '{"query": "有哪些组织"}'
```

## 前端示例（JavaScript）

```javascript
// 流式查询示例
async function streamQuery(query) {
  const response = await fetch('http://localhost:8000/query/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query: query })
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');

    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.slice(6));
        if (data.content) {
          console.log(data.content);
          // 更新 UI
        }
        if (data.done) {
          console.log('完成');
          break;
        }
      }
    }
  }
}
```

## 配置说明

数据库连接信息在 `bcnl.py` 中配置：

```python
host = '110.41.166.11'
port = '3306'
username = 'failedman'
password = '20230607'
database_schema = 'bcnl'
```

## 注意事项

- 确保数据库连接正常
- OpenAI API Key 需要在 `bcnl.py` 中配置
- 流式输出使用 Server-Sent Events (SSE) 格式

