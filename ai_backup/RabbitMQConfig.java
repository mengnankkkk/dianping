package com.mengnankk.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // 队列常量
    public static final String SECKILL_QUEUE = "seckill.queue";
    public static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_ROUTING_KEY = "seckill.order";
    public static final String EXCHANGE_NAME = SECKILL_EXCHANGE;
    
    // 延迟队列
    public static final String DELAY_QUEUE = "delay.queue";
    public static final String DELAY_EXCHANGE = "delay.exchange";
    
    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE).build();
    }
    
    @Bean
    public TopicExchange seckillExchange() {
        return new TopicExchange(SECKILL_EXCHANGE);
    }
    
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(SECKILL_ROUTING_KEY);
    }
    
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE).build();
    }
    
    @Bean
    public TopicExchange delayExchange() {
        return new TopicExchange(DELAY_EXCHANGE);
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}
