# 点评AI功能配置说明

## 前置准备

### 1. 数据库配置
确保MySQL数据库已经启动，并且配置正确：
- 数据库名：dianping
- 用户名：root
- 密码：123456
- 端口：3306

### 2. Redis配置
确保Redis服务已经启动：
- 主机：127.0.0.1
- 端口：6379

### 3. RabbitMQ配置
确保RabbitMQ服务已经启动：
- 主机：localhost
- 端口：5672
- 用户名/密码：guest/guest

## AI功能配置

### 1. 环境变量配置
在系统环境变量或IDE的运行配置中设置以下变量：

```bash
# 阿里云DashScope API Key（必须）
DASHSCOPE_API_KEY=your-actual-dashscope-api-key

# OpenAI API Key（可选，如果不使用OpenAI可以不设置）
OPENAI_API_KEY=your-openai-api-key

# OpenAI Base URL（可选）
OPENAI_BASE_URL=https://api.openai.com
```

### 2. DashScope API Key获取
1. 访问 [阿里云百炼平台](https://dashscope.aliyun.com/)
2. 注册并登录账号
3. 在控制台中创建API Key
4. 将API Key设置为环境变量 `DASHSCOPE_API_KEY`

### 3. 短信服务配置（可选）
如果需要使用短信功能，需要在 `application.yaml` 中配置：
```yaml
aliyun:
  sms:
    access-key-id: your_aliyun_access_key_id
    access-key-secret: your_aliyun_access_key_secret
    region-id: cn-hangzhou
    sign-name: 您的短信签名
    template-code-register: 您的注册模板Code
    template-code-reset-password: 您的重置密码模板Code
```

## 启动应用

### 1. 启动命令
```bash
cd e:\github\dianping
mvn spring-boot:run
```

或者在IDE中直接运行主类。

### 2. 验证服务
启动成功后，访问以下端点验证功能：

1. **应用健康检查**：
   - GET http://localhost:8081/api/ai/health

2. **AI聊天功能**：
   - POST http://localhost:8081/api/ai/chat
   - Body: "你好，请介绍一下你的功能"

3. **智能搜索功能**：
   - GET http://localhost:8081/api/ai/search?query=火锅

4. **WebSocket聊天**：
   - 连接：ws://localhost:8081/ws
   - 发送消息到：/app/chat.send

## API接口说明

### 1. AI聊天接口
```
POST /api/ai/chat
Content-Type: application/json

"你好，我想找一家好吃的火锅店"
```

### 2. 智能搜索接口
```
GET /api/ai/search?query=火锅
```

### 3. WebSocket聊天
```javascript
const socket = new SockJS('http://localhost:8081/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    // 订阅消息
    stompClient.subscribe('/topic/chat', function(message) {
        console.log('收到AI回复:', JSON.parse(message.body));
    });
    
    // 发送消息
    stompClient.send('/app/chat.send', {}, JSON.stringify({
        content: '你好'
    }));
});
```

## 功能特性

### 1. AI聊天服务
- 基于阿里云DashScope大模型
- 支持上下文对话
- 支持WebSocket实时聊天

### 2. 智能搜索服务
- 语义搜索 + 传统搜索融合
- AI重排序优化结果
- 向量存储支持

### 3. Agent系统
- 店铺推荐Agent
- 可扩展的工具调用框架

## 故障排除

### 1. 如果AI功能不工作
- 检查 `DASHSCOPE_API_KEY` 环境变量是否正确设置
- 检查网络连接是否正常
- 查看应用日志中的错误信息

### 2. 如果向量搜索不工作
- 确保向量数据已经初始化
- 检查Elasticsearch是否正常运行（如果使用）

### 3. 如果WebSocket连接失败
- 检查防火墙设置
- 确认端口8081没有被占用

## 开发建议

1. **数据初始化**：首次运行时建议导入一些测试店铺数据
2. **向量数据**：为了更好的搜索效果，需要为店铺数据生成向量嵌入
3. **监控日志**：关注应用日志，及时发现和解决问题
4. **性能优化**：根据实际使用情况调整AI调用频率和缓存策略
