package com.mengnankk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "chat.session")
public class ChatSessionConfig {
    
    // 会话配置常量
    public static final int MAX_HISTORY = 10;
    public static final long SESSION_TIMEOUT = 1800000; // 30分钟
    public static final int MAX_MESSAGE_LENGTH = 4000;
    
    // 可配置属性
    private int maxHistory = MAX_HISTORY;
    private long sessionTimeout = SESSION_TIMEOUT;
    private int maxMessageLength = MAX_MESSAGE_LENGTH;
    private boolean enableContextMemory = true;
    private boolean enableAutoSummary = false;
    
    // AI模型配置
    private String defaultModel = "qwen-turbo";
    private double temperature = 0.7;
    private int maxTokens = 2000;
}
