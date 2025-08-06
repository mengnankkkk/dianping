package com.mengnankk.entity.consumer;

import com.mengnankk.entity.CouponReceiveLog;
import com.mengnankk.service.CouponService;
import com.mengnankk.utils.MessageRetryHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.mengnankk.service.Impl.CouponServiceImpl.SECKILL_QUEUE;

@Slf4j
@Service
public class SeckillVoucherConsumer {
    @Autowired
    private CouponService couponService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private MessageRetryHandler messageRetryHandler;

    /**
     * 监听 seckill.queue 队列,
     */
    @org.springframework.amqp.rabbit.annotation.RabbitListener(queues = SECKILL_QUEUE)
    public void eceiveVoucherOrderMessage(CouponReceiveLog couponReceiveLog){
        log.info("接收到订单消息，用户id:{}, 优惠券id:{}, 订单id:{}", couponReceiveLog.getUserId(), couponReceiveLog.getVoucherId(), couponReceiveLog.getId());
        try{
            couponService.createVoucherOrder(couponReceiveLog);
        }catch (Exception e){
            log.error("失败进入重试");
        }
    }
}
