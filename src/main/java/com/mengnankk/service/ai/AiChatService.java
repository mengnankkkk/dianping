package com.mengnankk.service.ai;

import org.checkerframework.checker.units.qual.A;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI 聊天服务
 * 提供基于 Spring AI 的聊天功能
 */
@Service
public class AiChatService {
    
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    @Autowired
    private final OpenAiChatModel chatModel;
    
    @Autowired
    public AiChatService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    /**
     * 发送消息并获取 AI 回复
     * @param message 用户消息
     * @return AI 回复
     */
    public String chat(String message) {
        try {
            log.info("Sending message to AI: {}", message);
            
            Prompt prompt = new Prompt(message);
            ChatResponse response = chatModel.call(prompt);
            
            String aiReply = response.getResult().getOutput().getContent();
            log.info("Received AI reply: {}", aiReply);
            
            return aiReply;
        } catch (Exception e) {
            log.error("Error during AI chat: ", e);
            return "抱歉，AI 服务暂时不可用。";
        }
    }
    
    /**
     * 批量处理消息
     * @param messages 消息列表
     * @return AI 回复列表
     */
    public java.util.List<String> chatBatch(java.util.List<String> messages) {
        return messages.stream()
                .map(this::chat)
                .collect(java.util.stream.Collectors.toList());
    }
}
