package com.mengnankk.entity.consumer;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import static com.mengnankk.entity.mq.MQConstant.DEAD_LETTER_QUEUE;
@Component
@Slf4j
public class DeadLetterConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterConsumer.class);


    @RabbitListener(queues = DEAD_LETTER_QUEUE)
    public  void receive (String message){
        log.info("死信队列接收到消息"+message);
    }
}
