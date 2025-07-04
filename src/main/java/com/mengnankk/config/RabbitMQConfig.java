package com.mengnankk.config;

import com.mengnankk.entity.mq.MQConstant;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue basicInfoQueue() {
        return QueueBuilder.durable(MQConstant.QUEUE_BASIC_INFO).build();
    }

    @Bean
    public Queue inventoryQueue() {
        return QueueBuilder.durable(MQConstant.QUEUE_INVENTORY).build();
    }

    @Bean
    public Queue priceQueue() {
        return QueueBuilder.durable(MQConstant.QUEUE_PRICE).build();
    }

    @Bean
    public DirectExchange shopUpdateExchange() {
        return new DirectExchange(MQConstant.EXCHANGE_NAME);
    }

    @Bean
    public Binding basicInfoBinding(Queue basicInfoQueue, DirectExchange shopUpdateExchange) {
        return BindingBuilder.bind(basicInfoQueue).to(shopUpdateExchange).with(MQConstant.ROUTING_KEY);
    }

    @Bean
    public Binding inventoryBinding(Queue inventoryQueue, DirectExchange shopUpdateExchange) {
        return BindingBuilder.bind(inventoryQueue).to(shopUpdateExchange).with(MQConstant.ROUTING_KEY);
    }

    @Bean
    public Binding priceBinding(Queue priceQueue, DirectExchange shopUpdateExchange) {
        return BindingBuilder.bind(priceQueue).to(shopUpdateExchange).with(MQConstant.ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setMandatory(true); // 可选：开启回调机制
        return rabbitTemplate;
    }
    
}
