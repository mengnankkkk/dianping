package com.mengnankk.controller;

import com.alibaba.dashscope.common.Message;
import com.mengnankk.entity.ai.*;
import com.mengnankk.service.ai.EnterpriseChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {
    private final EnterpriseChatService chatService;
    private final ShopAiagnt shopAiagnt;

    /**
     * ai对话
     *
     * @param message
     * @param headerAccessor
     * @return
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/chat")
    public Object sendMessage(Message message, SimpMessageHeaderAccessor headerAccessor){
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.error("Session attributes is null");
            return Message.builder()
                    .content("会话信息丢失，请重新连接。")
                    .build();
        }
        
        String sessionId = (String) sessionAttributes.get("sessionId");
        String userId = (String) sessionAttributes.get("userId");

        try {
            ChatContext context = ChatContext.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    //.location(message.getLocation())
                    .build();

            ChatResponse response = chatService.sendMessage(sessionId, message.getContent(), context);
            return Message.builder()
                    .content(response.getResult().getOutput().getContent())
                    .build();
        }catch (Exception e){
            log.error("Error processing chat message: {}", e.getMessage(), e);
            return Message.builder()
                    .content("抱歉，我现在无法回答您的问题，请稍后重试。")
                    .build();
        }
    }

    /**
     * 调用agent
     * @param request
     * @param headerAccessor
     * @return
     */
    @MessageMapping("/agent.call")
    @SendTo("/topic/agent")
    public AgentResponse callAgent(AgentRequest request, SimpMessageHeaderAccessor headerAccessor){
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.error("Session attributes is null");
            return AgentResponse.builder()
                    .success(false)
                    .message("会话信息丢失，请重新连接。")
                    .build();
        }
        
        String userId = (String) sessionAttributes.get("userId");

        try {
            // 使用 ShopAgent 处理请求
            request.getContext().setUserId(Long.valueOf(userId));
            return shopAiagnt.execute(request);
        }catch (Exception e){
            log.error("Error calling agent {}: {}", request.getRequestId(), e.getMessage(), e);
            return AgentResponse.builder()
                    .success(false)
                    .message("Agent执行失败: " + e.getMessage())
                    .build();
        }
    }

}
