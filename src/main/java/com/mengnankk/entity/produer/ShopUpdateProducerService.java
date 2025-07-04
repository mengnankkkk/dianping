package com.mengnankk.entity.produer;

import cn.hutool.json.JSONUtil;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.mq.OutboxMessage;
import com.mengnankk.entity.Shop;
import com.mengnankk.entity.mq.ShopUpdateMessage;
import com.mengnankk.mapper.OutboxMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ShopUpdateProducerService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OutboxMessageMapper outboxMessageMapper;

    @Value("${mq.batch.size}")
    private int batchSize;
    @Value("${mq.batch.frequency}")
    private int frequency;
    private static final int MAX_SEND_RETRY_COUNT = 5;
    private static final String SHOP_UPDATE_TOPIC = "shop.update.topic";

    private  final  List<OutboxMessage> batchMessages = new ArrayList<>();

    /**
     * 定时批量发送
     */
    @Scheduled(fixedDelayString = "${mq.batch.frequency}000")
    public void batchMessages(){
        if (!batchMessages.isEmpty()){
            List<OutboxMessage> messagestosend = new ArrayList<>(batchSize);
            batchMessages.clear();
            sendBatchMessages(messagestosend);
        }
    }
    // 发送单个消息
    @Transactional
    public void sendShopUpdateMessage(ShopUpdateMessage message) {
        sendShopUpdateMessage(message);
    }

    /**
     * 线程安全添加消息
     * @param outboxMessage
     */
    public synchronized void addMessageToBatch(OutboxMessage  outboxMessage){
        batchMessages.add(outboxMessage);
        if (batchMessages.size()>batchSize){
            List<OutboxMessage> messagestosend = new ArrayList<>(batchSize);
            batchMessages.clear();
            sendBatchMessages(messagestosend);
        }
    }

    /**
     * 发送店铺请求
     * @param shop
     * @return
     */
    @Transactional
    public Result requestShopUpdate(Shop shop){
        if (shop==null||shop.getId()==null){
            return Result.fail("店铺信息为null");
        }

        String messageId = UUID.randomUUID().toString();
        ShopUpdateMessage message = new ShopUpdateMessage();
        message.setMessageId(messageId);
        message.setShopId(shop.getId());
        message.setShopData(shop);
        message.setTimestamp(System.currentTimeMillis());

        OutboxMessage outboxMessage = new OutboxMessage(messageId,SHOP_UPDATE_TOPIC, JSONUtil.toJsonStr(message),OutboxMessage.Status.PENDING);
        outboxMessageMapper.insert(outboxMessage);
        log.info("店铺更新请求已提交，消息ID: {}，等待异步发送。", messageId);

        return Result.ok("店铺更新请求已提交，请稍后查询结果。");

    }

    /**
     * 批量发送
     * @param messages
     */
    private void sendBatchMessages(List<OutboxMessage> messages){
        log.info("开始批量发送"+messages.size());
        for (OutboxMessage msg :messages){
            try {
                log.info("消息内容"+msg.toString());
                rabbitTemplate.convertAndSend(SHOP_UPDATE_TOPIC,msg.getPayload());
                msg.setStatus(OutboxMessage.Status.SENT);
                outboxMessageMapper.updateById(msg);
                log.info("\"消息ID: {} 成功发送到MQ。\", msg.getMessageId()");

            }catch (Exception e){
                log.error("发送消息ID: {} 到MQ失败: {}", msg.getId(), e.getMessage(), e);
                msg.setRetryCount(msg.getRetryCount()+1);
                msg.setStatus(OutboxMessage.Status.FAILED);
                outboxMessageMapper.updateById(msg);
            }
        }
    }

    /**
     * 定时发送
     */
    @Scheduled(fixedDelay = 5000)
    public void sendPendingMessages(){
        List<OutboxMessage> pendingMessages = outboxMessageMapper.selectByStatus(OutboxMessage.Status.PENDING);
        for (OutboxMessage msg:pendingMessages){
            try{
                rabbitTemplate.convertAndSend(SHOP_UPDATE_TOPIC,msg.getPayload());
                msg.setStatus(OutboxMessage.Status.SENT);
                outboxMessageMapper.updateById(msg);
                log.info("消息ID: {} 成功发送到MQ。", msg.getId());
            }catch (Exception e){
                log.error("发送消息ID: {} 到MQ失败: {}", msg.getId(), e.getMessage(), e);
                msg.setRetryCount(msg.getRetryCount()+1);
                if (msg.getRetryCount()>MAX_SEND_RETRY_COUNT){
                    msg.setStatus(OutboxMessage.Status.FAILED);
                }
                outboxMessageMapper.updateById(msg);
            }
        }
    }
}
