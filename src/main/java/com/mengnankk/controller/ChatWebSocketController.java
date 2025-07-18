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
        String sessionId = (String) headerAccessor.getSessionAttributes().get("sessionId");
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");

        try {
            ChatContext context = ChatContext.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    //.location(message.getLocation())
                    .build();

            ChatResponse response =chatService.sendMessage(sessionId,message.getContent(),context);
            return Message.builder()
                    .content(response.getResult().getOutput().getContent())
                    .build();
        }catch (Exception e){
            log.error("Error processing chat message: {}", e.getMessage(), e);
            return Message.builder()
                    .content("抱歉，我现在无法回答您的问题，请稍后重试。");
        }
    }

    /**
     * 调用agent
     * @param request
     * @param headerAccessor
     * @return
     */
    @MessageMapping("/agnet.call")
    @SendTo("/topic/agent")
    public AgentResponse callAgent(AgentRequest request,SimpMessageHeaderAccessor headerAccessor){
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");

        try {
            //加载agnet
            AiAgent aiAgent = null;
            request.getContext().setUserId(Long.valueOf(userId));
            return aiAgent.execute(request);
        }catch (Exception e){
            log.error("Error calling agent {}: {}", request.getRequestId(), e.getMessage(), e);
            return AgentResponse.builder()
                    .success(false)
                    .message("Agent执行失败: " + e.getMessage())
                    .build();
        }
    }

}
