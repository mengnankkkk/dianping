package com.mengnankk.entity.mq;

public class MQConstant {
    // 交换机名称
    public static final String EXCHANGE_NAME = "shop.update.exchange";
    // 队列名称
    public static final String QUEUE_BASIC_INFO = "shop.update.basic_info.queue";
    public static final String QUEUE_INVENTORY = "shop.update.inventory.queue";
    public static final String QUEUE_PRICE = "shop.update.price.queue";
    public static final String QUEUE_SECKILL = "seckill.queue";
    // 路由键
    public static final String ROUTING_KEY = "shop.update";
    // 死信交换机名称
    public static final String DEAD_LETTER_EXCHANGE = "dead.letter.exchange";
    // 死信队列名称
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead.letter.routingKey";

}