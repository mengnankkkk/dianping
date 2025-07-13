package com.mengnankk.utils;

import com.mengnankk.entity.mq.RetryMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

@Component
@Slf4j
public class MessageRetryHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper; // Jackson for JSON serialization/deserialization

    // 配置选项 (从配置文件读取)
    @Value("${message.retry.redis.lock.expiration:60}")
    private long redisLockExpirationSeconds;
    @Value("${message.retry.max.attempts:3}")
    private int maxRetryAttempts;
    @Value("${message.retry.interval:60000}")
    private long retryIntervalMillis;

    private static final String RETRY_LOCK_PREFIX = "retry:lock:";
    private static final String RETRY_COUNT_PREFIX = "retry:count:"; // 记录重试次数
    private static final String RETRY_PAYLOAD_PREFIX = "retry:payload:"; // 存储消息内容
    private static final String DEAD_LETTER_EXCHANGE = "dead.letter.exchange";

    /**
     * 处理重试消息。 通用的重试方法，无需消息自己实现retryMessage
     *
     * @param exchange  Exchange Name
     * @param routingKey Routing Key
     * @param payload   原始消息内容（需要是JSON字符串）
     * @param messageId  消息Id
     */
    public void handleRetry(String exchange, String routingKey, String payload, String messageId) {
        //String messageId = UUID.randomUUID().toString(); // 生成唯一消息ID
        // 从Redis中获取重试次数
        String retryCountKey = RETRY_COUNT_PREFIX + messageId;
        Integer retryCount = getRetryCount(messageId);
        if (retryCount == null) {
            retryCount = 0;
        }
        String lockKey = RETRY_LOCK_PREFIX + messageId;
        if (retryCount >= maxRetryAttempts) {
            //  消息超过最大重试次数，发送到死信队列，并清理Redis数据
            log.warn("消息超过最大重试次数，消息ID：{}，发送到死信队列", messageId);
            clearRetryData(messageId); // 清理 Redis 数据
            rabbitTemplate.convertAndSend(DEAD_LETTER_EXCHANGE, routingKey, payload); //使用原始routingKey
            return;
        }
        // Redis分布式锁,
        String requestId = UUID.randomUUID().toString(); // 唯一ID
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, requestId, redisLockExpirationSeconds, TimeUnit.SECONDS);
        if (lockAcquired != null && lockAcquired) {
            try {
                //增加重试次数
                Long newRetryCount = redisTemplate.opsForValue().increment(retryCountKey);
                if (newRetryCount == null) {
                    // 理论上不可能发生，因为lockAcquired成功，说明已经初始化了
                    log.error("重试次数初始化失败，消息ID：{}", messageId);
                    return;
                }
                //发送消息
                rabbitTemplate.convertAndSend(exchange, routingKey, payload);
                log.info("消息重试成功，消息ID：{}, 重试次数：{}", messageId, newRetryCount);
            } catch (Exception e) {
                log.error("消息重试出现异常，消息ID：{}，异常信息：{}", messageId, e.getMessage(), e);
            } finally {
                releaseLock(lockKey,requestId);// 释放锁
            }
        } else {
            log.warn("获取Redis短期锁失败，稍后重试，消息ID：{}", messageId);
        }
    }

    /**
     * 释放锁
     * @param lockKey
     * @param requestId
     */
    private void releaseLock(String lockKey, String requestId) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            // 使用 RedisScript 封装 Lua 脚本
            RedisScript<Long> redisScript = RedisScript.of(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

            Long result = redisTemplate.execute(redisScript, List.of(lockKey), requestId);
            if (result != null && result > 0) {
                log.debug("锁已释放，key：{}", lockKey);
            } else {
                log.warn("未能释放锁，key：{}", lockKey);
            }
        }
    }


    /**
     * 获取重试次数
     *
     * @param messageId 消息ID
     * @return 重试次数
     */
    public Integer getRetryCount(String messageId) {
        String retryCountKey = RETRY_COUNT_PREFIX + messageId;
        String retryCountStr = redisTemplate.opsForValue().get(retryCountKey);
        if (StringUtils.hasText(retryCountStr)) {
            try {
                return Integer.parseInt(retryCountStr);
            } catch (NumberFormatException e) {
                log.error("重试次数转换失败，消息ID：{}，值：{}", messageId, retryCountStr);
                return null;
            }
        }
        return null;
    }

    /**
     * 清理Redis数据 (重试次数、消息内容等)
     * @param messageId
     */
    private void clearRetryData(String messageId) {
        String retryCountKey = RETRY_COUNT_PREFIX + messageId;
        String retryPayloadKey = RETRY_PAYLOAD_PREFIX + messageId;

        List<String> keysToDelete = new ArrayList<>();
        keysToDelete.add(retryCountKey);
        keysToDelete.add(retryPayloadKey);

        redisTemplate.delete(keysToDelete);
        log.info("已清理消息重试数据，消息ID：{}", messageId);
    }

    /**
     * 定时任务：扫描需要重试的消息
     */
    @Scheduled(fixedDelayString = "${message.retry.interval:60000}")
    public void retryMessages() {

        log.info("开始扫描待重试消息...");
        // 获取所有匹配 "retry:payload:*" 的 key
        List<String> payloadKeys = new ArrayList<>();
        redisTemplate.keys(RETRY_PAYLOAD_PREFIX + "*").forEach(payloadKeys::add);

        if (payloadKeys.isEmpty()) {
            log.info("没有待重试的消息");
            return;
        }

        for (String payloadKey : payloadKeys) {
            try {
                //提取messageId
                String[] parts = payloadKey.split(":");
                String messageId = parts[parts.length - 1];
                // 从 Redis 中获取 payload
                String payloadStr = redisTemplate.opsForValue().get(payloadKey);
                //判断payloadStr不为空
                if (!StringUtils.hasText(payloadStr)){
                    log.warn("缓存中找不到消息内容，messageId:{}",messageId);
                    continue; //如果消息内容为空的情况，跳过；
                }
                // 从 Redis 中获取 Exchange、RoutingKey
                // 从 Payload 中获取 Exchange、RoutingKey 一般约定json格式：{"exchange":"your.exchange","routingKey":"your.routing.key","data":{}}
                RetryMessage retryMessageWrapper = null;
                try {
                    retryMessageWrapper = objectMapper.readValue(payloadStr, RetryMessage.class);
                }catch (Exception e){
                    log.error("消息内容转换失败，跳过messageid:{}",messageId,e);
                    continue; //跳过
                }

                String exchange = retryMessageWrapper.getExchange();
                String routingKey = retryMessageWrapper.getRoutingKey();
                String data = retryMessageWrapper.getData();

                // 执行重试
                handleRetry(exchange, routingKey, data,messageId);

            } catch (Exception e) {
                log.error("扫描重试消息发生异常", e);
            }
        }
        log.info("扫描待重试消息完成");
    }
}

