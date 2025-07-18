package com.mengnankk.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.aliyun.credentials.AlibabaCloudCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * AI 配置类
 * 为 Spring AI 提供基本配置
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(AlibabaCloudCredentials.class)
public class AiConfig {
    @Bean
    @Primary
    public ChatModel dashScopeChatModel(DashScopeChatModel dashScopeChatModel){
        log.info("启用阿里ai");
        return dashScopeChatModel;
    }
    @Bean
    @Primary
    public EmbeddingModel dashScopeEmbeddingModel(DashScopeEmbeddingModel dashScopeEmbeddingModel){
        log.info("Initializing DashScope Embedding Model");
        return dashScopeEmbeddingModel;
    }


}
