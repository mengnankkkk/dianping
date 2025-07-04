package com.mengnankk.entity.mq;

import com.mengnankk.entity.Shop;
import lombok.Data;

import java.awt.*;
import java.io.Serializable;
import java.util.Map;

@Data
public class ShopUpdateMessage extends BaseMessage implements Serializable {
    private String messageId; // 全局唯一消息ID，由生产者生成
    private Long shopId;      // 更新的店铺ID
    private Shop shopData;    // 更新后的店铺完整数据或部分字段
    private long timestamp;   // 消息生成时间戳
    private int priority;
    private String transactionId;
    int retryCount; // 重试次数，用于MQ内部管理或消费者侧判断
    Map<String, String> headers; // 扩展头部信息
    private String  shopInfo; //Shop 信息的JSON字符串

}
