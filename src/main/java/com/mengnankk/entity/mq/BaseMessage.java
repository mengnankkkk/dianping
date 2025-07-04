package com.mengnankk.entity.mq;

import cn.hutool.core.lang.UUID;

import java.io.Serializable;
import java.util.Map;

public class BaseMessage implements Serializable {
    private String messageId;
    private long timestamp;
    private int priority;
    private String transactionId;
    int retryCount; // 重试次数，用于MQ内部管理或消费者侧判断
    Map<String, String> headers; // 扩展头部信息
    private String  shopInfo; //Shop 信息的JSON字符串
    public BaseMessage() {
        this.messageId = UUID.randomUUID().toString();
    }
    public String getMessageId() {
        return messageId;
    }
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    @Override
    public String toString() {
        return "BaseMessage{" +
                "messageId='" + messageId + '\'' +
                '}';
    }
}
