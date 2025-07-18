package com.mengnankk.entity.consumer;

import com.mengnankk.entity.Shop;
import com.mengnankk.entity.mq.ShopUpdateMessage;
import com.mengnankk.service.Impl.MessageLogService;
import com.mengnankk.service.Impl.ShopServiceImpl;
import com.mengnankk.service.ShopService;
import com.mengnankk.utils.AsyncDbRedisWriter;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mengnankk.utils.RedisConstants.*;

import jakarta.annotation.Resource;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.mengnankk.entity.mq.MQConstant.QUEUE_BASIC_INFO;

@Component
@Slf4j
public class ShopUpdateConsumer extends AbstractShopUpdateConsumer{
    private static final Logger logger = LoggerFactory.getLogger(ShopUpdateConsumer.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MessageLogService messageLogService;
    @Autowired
    private ShopService shopService;

    private static final int MAX_RETRIES = 3;
    private  static  final int DELAY_SECONDS = 1;

    @Override
    @RabbitListener(queues = QUEUE_BASIC_INFO)
    @Transactional(rollbackFor = Exception.class)
    public void consume(ShopUpdateMessage message){
        String  messageId = message.getMessageId();
        try {
            if (isMessageProcessed(messageId)){
                log.warn("Message {} already processed, ignoring.", messageId);
                return;
            }
            Shop shop =message.getShopData();
            if (shop==null){
                log.warn("Shop data is null in message {}", messageId);
                return;
            }
            AsyncDbRedisWriter.write(shop,
                    s->shopService.getBaseMapper().updateById(s),
                    "cache:shopType:"+shop.getId());
            markMessageAsProcessed(messageId);
            log.info("Message {} processed successfully.", messageId);
        }catch (Exception e){
            log.error("Error processing message: {}, retrying...", messageId, e);
            retryMessage(message,MAX_RETRIES);
            log.error("Message {} processing failed after maximum retries.", messageId, e);
            throw new RuntimeException("消费失败",e);
        }
    }

    private boolean isMessageProcessed(String messageId){
        return stringRedisTemplate.hasKey("processed"+messageId);
    }
    private void markMessageAsProcessed(String messageId) {
        stringRedisTemplate.opsForValue().set("processed:" + messageId, "true", 1, TimeUnit.DAYS);
    }
    private void retryMessage(ShopUpdateMessage message, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                TimeUnit.SECONDS.sleep(DELAY_SECONDS);
                consume(message);
                return; // 重试成功，退出循环
            } catch (Exception e) {
                log.error("Retry {} failed for message: {}", i + 1, message.getMessageId(), e);
            }
        }
    }

}
