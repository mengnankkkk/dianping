package com.mengnankk.service.Impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class MessageLogService {
    private static final Logger logger = LoggerFactory.getLogger(MessageLogService.class);

    private final Map<String, Map<String, Object>> messageLogs = new HashMap<>();
    public void logMessage(String messageId, String messageContent, String exchange, String routingKey) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("messageId", messageId);
        logEntry.put("messageContent", messageContent);
        logEntry.put("exchange", exchange);
        logEntry.put("routingKey", routingKey);
        logEntry.put("sendTime", new Date());
        logEntry.put("status", "SENT");
        messageLogs.put(messageId, logEntry);
        logger.info("Message sent: {}", logEntry);
    }
    public void logMessageConsumeSuccess(String messageId) {
        if (messageLogs.containsKey(messageId)) {
            Map<String, Object> logEntry = messageLogs.get(messageId);
            logEntry.put("consumeTime", new Date());
            logEntry.put("status", "CONSUMED");
            logger.info("Message consumed successfully: {}", logEntry);
        } else {
            logger.warn("Message {} not found in logs.", messageId);
        }
    }
    public void logMessageConsumeFail(String messageId) {
        if (messageLogs.containsKey(messageId)) {
            Map<String, Object> logEntry = messageLogs.get(messageId);
            logEntry.put("consumeTime", new Date());
            logEntry.put("status", "FAILED");
            logger.error("Message consume failed: {}", logEntry);
        } else {
            logger.warn("Message {} not found in logs.", messageId);
        }
    }
}
