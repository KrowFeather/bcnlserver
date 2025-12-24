from langchain_community.utilities import SQLDatabase
from langchain_core.prompts.chat import HumanMessagePromptTemplate, SystemMessagePromptTemplate, ChatPromptTemplate
from langchain_community.chat_models import ChatOpenAI
from langchain_core.messages import SystemMessage

OPENAI_API_KEY = "sk-Zody2hNl5Q1QrmnqB62c822085C641949a80A6C21aE548Ca"
llm = ChatOpenAI(temperature=0, openai_api_key=OPENAI_API_KEY, openai_api_base="https://api.ai-gaochao.cn/v1")

host = '110.41.166.11'
port = '3306'
username = 'failedman'
password = '20230607'
database_schema = 'bcnl'
mysql_uri = f"mysql+pymysql://{username}:{password}@{host}:{port}/{database_schema}"

db = SQLDatabase.from_uri(mysql_uri, sample_rows_in_table_info=2)

# 创建 SQL 生成提示
sql_prompt = ChatPromptTemplate.from_messages([
    SystemMessagePromptTemplate.from_template(
        """你是一个SQL专家。根据用户的问题和数据库表结构，生成SQL查询语句。
        
数据库表结构：
{dialect}

表信息：
{table_info}

只返回SQL查询语句，不要返回其他内容。"""
    ),
    HumanMessagePromptTemplate.from_template("{input}")
])


def retrieve_from_db(query: str) -> str:
    # 生成 SQL 查询
    sql_chain = sql_prompt | llm
    sql_response = sql_chain.invoke({
        "input": query,
        "dialect": db.dialect,
        "table_info": db.get_table_info()
    })

    # 提取 SQL 语句（去除可能的 markdown 代码块标记）
    sql_query = sql_response.content.strip()
    if sql_query.startswith("```sql"):
        sql_query = sql_query[6:]
    if sql_query.startswith("```"):
        sql_query = sql_query[3:]
    if sql_query.endswith("```"):
        sql_query = sql_query[:-3]
    sql_query = sql_query.strip()

    print(f"DEBUG: 生成的SQL查询: {sql_query}")

    # 执行 SQL 查询
    try:
        query_result = db.run(sql_query)
        print(f"DEBUG: 查询结果: {query_result}")

        # 组合 SQL 查询和结果
        db_context = f"SQL查询: {sql_query}\n查询结果: {query_result}"
    except Exception as e:
        print(f"DEBUG: SQL执行错误: {e}")
        db_context = f"SQL查询: {sql_query}\n查询错误: {str(e)}"

    db_context = db_context.strip()
    print(f"DEBUG: 数据库查询结果: {db_context}")  # 调试输出
    return db_context


def generate(query: str) -> str:
    db_context = retrieve_from_db(query)

    # 如果查询结果为空，直接返回提示
    if not db_context or db_context == "":
        return "抱歉，没有找到相关的数据库信息。"

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
    print(f"DEBUG: 发送给LLM的上下文: {db_context[:200]}...")  # 调试输出
    response = llm.invoke(messages)
    return response.content


if __name__ == '__main__':
    query = "有哪些组织"
    print(generate(query))
