from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import json
import asyncio
from bcnl import retrieve_from_db, llm, SystemMessage, HumanMessagePromptTemplate

app = FastAPI(title="BCNL Database QA API", version="1.0.0")

# 添加 CORS 中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"],  # 允许的前端地址
    allow_credentials=True,
    allow_methods=["*"],  # 允许所有方法
    allow_headers=["*"],  # 允许所有请求头
)


class QueryRequest(BaseModel):
    query: str


async def generate_stream(query: str):
    """流式生成回答"""
    try:
        # 第一步：获取数据库上下文
        db_context = retrieve_from_db(query)
        
        if not db_context or db_context == "":
            yield f"data: {json.dumps({'content': '抱歉，没有找到相关的数据库信息。', 'done': True}, ensure_ascii=False)}\n\n"
            return
        
        system_message = """You are a professional database qa agency.
        You have to answer user's queries from the provided evidences and in a kind form.
        Always include the specific data from the Context in your answer.
        """
        
        human_qry_template = HumanMessagePromptTemplate.from_template(
            """用户问题:
        {human_input}

        数据库查询结果:
        {db_context}

        请基于上述数据库查询结果，用友好的方式回答用户的问题。不要输出数据里面含有的链接，被删除和未通过的信息不要输出
        """
        )
        
        messages = [
            SystemMessage(content=system_message),
            human_qry_template.format(human_input=query, db_context=db_context)
        ]
        
        print(f"DEBUG: 发送给LLM的上下文: {db_context[:200]}...")
        
        # 使用流式调用 LLM
        from concurrent.futures import ThreadPoolExecutor
        import queue
        
        # 使用队列在线程间传递数据
        result_queue = queue.Queue()
        stream_finished = False
        
        def sync_stream():
            """同步流式生成"""
            nonlocal stream_finished
            try:
                for chunk in llm.stream(messages):
                    content = chunk.content if hasattr(chunk, 'content') else str(chunk)
                    if content:
                        result_queue.put(('content', content))
                stream_finished = True
                result_queue.put(('done', None))
            except Exception as e:
                stream_finished = True
                result_queue.put(('error', str(e)))
        
        # 在线程池中执行同步流式生成
        loop = asyncio.get_event_loop()
        executor = ThreadPoolExecutor(max_workers=1)
        
        try:
            # 启动流式生成任务
            future = loop.run_in_executor(executor, sync_stream)
            
            # 从队列中获取数据并流式输出
            has_error = False
            while True:
                try:
                    # 从队列获取数据，设置超时避免无限等待
                    try:
                        msg_type, data = result_queue.get(timeout=1.0)
                    except queue.Empty:
                        # 如果队列为空且流已完成，则退出
                        if stream_finished:
                            break
                        continue
                    
                    if msg_type == 'content':
                        yield f"data: {json.dumps({'content': data, 'done': False}, ensure_ascii=False)}\n\n"
                    elif msg_type == 'error':
                        yield f"data: {json.dumps({'content': f'[错误: {data}]', 'done': True, 'error': True}, ensure_ascii=False)}\n\n"
                        has_error = True
                        break
                    elif msg_type == 'done':
                        break
                        
                except Exception as e:
                    yield f"data: {json.dumps({'content': f'[流式输出错误: {str(e)}]', 'done': True, 'error': True}, ensure_ascii=False)}\n\n"
                    has_error = True
                    break
            
            # 确保任务完成
            try:
                await asyncio.wait_for(future, timeout=5.0)
            except asyncio.TimeoutError:
                pass
            
        except Exception as e:
            yield f"data: {json.dumps({'content': f'[处理错误: {str(e)}]', 'done': True, 'error': True}, ensure_ascii=False)}\n\n"
        finally:
            executor.shutdown(wait=True)
        
        # 发送完成标记
        if not has_error:
            yield f"data: {json.dumps({'content': '', 'done': True}, ensure_ascii=False)}\n\n"
        
    except Exception as e:
        error_msg = f"处理请求时发生错误: {str(e)}"
        print(f"ERROR: {error_msg}")
        yield f"data: {json.dumps({'content': error_msg, 'done': True, 'error': True}, ensure_ascii=False)}\n\n"


@app.options("/query/stream")
async def options_query_stream():
    """处理预检请求"""
    return {"message": "OK"}


@app.post("/query/stream")
async def query_stream(request: QueryRequest):
    """流式查询接口"""
    return StreamingResponse(
        generate_stream(request.query),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"
        }
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

