"""
测试客户端 - 用于测试流式输出 API
"""
import requests
import json
import sys

def test_stream_api(query: str, base_url: str = "http://localhost:8000"):
    """测试流式 API"""
    url = f"{base_url}/query/stream"
    data = {"query": query}
    
    print(f"发送查询: {query}")
    print("=" * 50)
    
    try:
        response = requests.post(url, json=data, stream=True)
        response.raise_for_status()
        
        full_content = ""
        for line in response.iter_lines():
            if line:
                line_str = line.decode('utf-8')
                if line_str.startswith('data: '):
                    try:
                        data_str = line_str[6:]  # 移除 'data: ' 前缀
                        data_json = json.loads(data_str)
                        content = data_json.get('content', '')
                        done = data_json.get('done', False)
                        
                        if content:
                            print(content, end='', flush=True)
                            full_content += content
                        
                        if done:
                            print("\n" + "=" * 50)
                            print("流式输出完成")
                            break
                    except json.JSONDecodeError as e:
                        print(f"\n解析错误: {e}")
                        print(f"原始数据: {line_str}")
        
        return full_content
        
    except requests.exceptions.RequestException as e:
        print(f"请求错误: {e}")
        return None


def test_normal_api(query: str, base_url: str = "http://localhost:8000"):
    """测试普通 API"""
    url = f"{base_url}/query"
    data = {"query": query}
    
    print(f"发送查询: {query}")
    print("=" * 50)
    
    try:
        response = requests.post(url, json=data)
        response.raise_for_status()
        result = response.json()
        print(result['response'])
        print("=" * 50)
        return result['response']
    except requests.exceptions.RequestException as e:
        print(f"请求错误: {e}")
        return None


if __name__ == "__main__":
    if len(sys.argv) > 1:
        query = " ".join(sys.argv[1:])
    else:
        query = "有哪些组织"
    
    print("测试流式 API:")
    test_stream_api(query)
    # print("\n")
    # print("测试普通 API:")
    # test_normal_api(query)

