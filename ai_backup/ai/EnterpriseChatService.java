package com.mengnankk.service.ai;

import com.alibaba.dashscope.common.Message;
import com.mengnankk.config.ChatSessionConfig;
import com.mengnankk.entity.ai.ChatContext;
import com.mengnankk.entity.ai.ChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnterpriseChatService {

    private final ChatModel chatModel;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, ChatSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * 创建对话
     * @param userId
     * @param config
     * @return
     */
    public ChatSession createSession(String userId, ChatSessionConfig config) {
        String sessionId = UUID.randomUUID().toString();
        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .config(config)
                .messages(new ArrayList<>())
                .createdAt(new Date())
                .lastActiveAt(new Date())
                .build();
        activeSessions.put(sessionId, session);
        cacheSession(session);
        log.info("Created chat session {} for user {}", sessionId, userId);
        return session;
    }

    /**
     * 发送消息
     * @param sessionId
     * @param message
     * @param context
     * @return
     */
    public ChatResponse sendMessage(String sessionId, String message, ChatContext context) {
        ChatSession session = getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }

        try {
            String enhancedPrompt = buildEnhancedPrompt(message, session, context);
            Prompt prompt = new Prompt(enhancedPrompt);
            ChatResponse response = chatModel.call(prompt);

            saveMessageHistory(session, message, response.getResult().getOutput().getContent());
            updateSession(session);
            return response;
        } catch (Exception e) {
            log.error("Error processing message in session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to process message", e);
        }
    }

    /**
     * 增强对话
     * @param userMessage
     * @param session
     * @param context
     * @return
     */
    private String buildEnhancedPrompt(String userMessage, ChatSession session, ChatContext context) {
        PromptTemplate template = new PromptTemplate("""
                是一个智能客服助手，专门为点评平台提供服务。
                
                用户信息：
                - 用户ID: {userId}
                - 会话历史: {history}
                
                上下文信息：
                - 当前位置: {location}
                - 搜索偏好: {preferences}
                - 业务上下文: {businessContext}
                
                用户问题: {userMessage}
                
                请根据以上信息，提供准确、有帮助的回答。如果需要查询具体信息，请说明需要调用哪些工具函数。
                """);

        Map<String, Object> variables = Map.of(
                "userId", session.getUserId(),
                "history", formatHistory(session.getMessages()),
                "location", context.getLocation(),
                "preferences", context.getPreferences(),
                "businessContext", context.getBusinessContext(),
                "userMessage", userMessage
        );
        return template.render(variables);
    }

    /**
     * 格式化历史信息
     * @param messages
     * @return
     */
    private String formatHistory(List<Message> messages) {
        return messages.stream()
                .limit(10)
                .map(msg -> String.format("[%s] %s", msg.getClass().getSimpleName(), msg.getContent()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 获取会话
     * @param sessionId
     * @return
     */

    public ChatSession getSession(String sessionId) {
        ChatSession session = activeSessions.get(sessionId);
        if (session == null) {
            session = loadSessionFromCache(sessionId);
            if (session != null) {
                activeSessions.put(sessionId, session);
            }
        }
        return session;
    }

    /**
     * 保存对话
     * @param session
     * @param userMessage
     * @param aiResponse
     */
    private void saveMessageHistory(ChatSession session, String userMessage, String aiResponse) {
        Message user = Message.builder().role("user").content(userMessage).build();
        Message assistant = Message.builder().role("assistant").content(aiResponse).build();

        session.getMessages().add(user);
        session.getMessages().add(assistant);

        if (session.getMessages().size() > 40) {
            session.getMessages().subList(0, 20).clear();
        }
    }

    private void updateSession(ChatSession session) {
        session.setLastActiveAt(new Date());
        cacheSession(session);
    }

    private void cacheSession(ChatSession session) {
        String key = "chat:session:" + session.getSessionId();
        redisTemplate.opsForValue().set(key, session, Duration.ofMinutes(30));
    }

    private ChatSession loadSessionFromCache(String sessionId) {
        String key = "chat:session:" + sessionId;
        return (ChatSession) redisTemplate.opsForValue().get(key);
    }
}
