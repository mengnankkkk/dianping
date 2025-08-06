package com.mengnankk.entity.ai;

import com.alibaba.dashscope.common.Message;
import com.mengnankk.config.ChatSessionConfig;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.model.ChatModel;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Data
@Builder
public class ChatSession  implements Serializable {
    private String sessionId;
    private String userId;
    private ChatSessionConfig config;
    private List<Message> messages;
    private Date createdAt;
    private Date lastActiveAt;
}