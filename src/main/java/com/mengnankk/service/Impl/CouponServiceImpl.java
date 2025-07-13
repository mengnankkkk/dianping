package com.mengnankk.service.Impl;

import cn.hutool.core.io.resource.ClassPathResource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.Coupon;
import com.mengnankk.entity.CouponReceiveLog;
import com.mengnankk.mapper.CouponMapper;
import com.mengnankk.service.CouponReceiveLogService;
import com.mengnankk.service.CouponService;
import com.mengnankk.service.SeckillVoucherService;
import com.mengnankk.utils.AuthContextHolder;
import com.mengnankk.utils.RedisIDWoker;
import com.sleepycat.je.utilint.TaskCoordinator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.RedissonMultiLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.stream.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CouponServiceImpl extends ServiceImpl<CouponMapper,Coupon> implements CouponService {
    private static final String COUPON_RECEIVED_KEY_PREFIX = "user:coupon:received:";
    //优惠券前缀
    private static final String COUPON_STOCK_KEY_PREFIX = "coupon:stock:";
    private static final String SECKILL_VOUCHER_ORDER = "seckill:voucher:order";

    private static final String SECKILL_EXCHANGE = "seckill.exchange";
    public static final String SECKILL_QUEUE = "seckill.queue";
    private static final String SECKILL_KEY = "seckill.key";

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private SeckillVoucherService seckillVoucherService;

    @Autowired
    private CouponReceiveLogService couponReceiveLogService;
    @Autowired
    private RedisIDWoker redisIdWorker;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation((Resource) new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 优惠劵秒杀
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        long userid = AuthContextHolder.getUserId();
        String userIDString  = String.valueOf(userid);
        Long result = null;
        try {
            result  =redisTemplate.execute(
                    SECKILL_SCRIPT,
                    Collections.emptyList(),
                    voucherId.toString(),
                    userIDString
            );
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        if (result!=null&&!result.equals(0L)){
            int r = result.intValue();
            return Result.fail(r==2?"不能重复下单":"库存不足");
        }

        long orderid = redisIdWorker.nextId(SECKILL_VOUCHER_ORDER);
        CouponReceiveLog couponReceiveLog = new CouponReceiveLog();
        couponReceiveLog.setVoucherId(voucherId);
        couponReceiveLog.setUserId(userid);
        couponReceiveLog.setCouponType("seckill");
        couponReceiveLog.setReceiveTime(LocalDateTime.now());
        couponReceiveLog.setId(orderid);
        couponReceiveLog.setStatus(0);

        boolean add = couponReceiveLogService.addSeckillTast(couponReceiveLog);
        if (!add){
            return Result.fail("已售空");
        }
        try {
            rabbitTemplate.convertAndSend(SECKILL_EXCHANGE,SECKILL_KEY,couponReceiveLog);
            log.info("订单消息发送成功!订单id: {}", orderid);

        }catch (Exception e){
            log.error("订单消息发送异常!订单id: {}", orderid, e);
            return Result.fail("秒杀失败！服务器繁忙！");
        }
        return Result.ok(orderid);
    }

    /**
     * 下单
     */
    @Override
    @Transactional
    public Result createVoucherOrder(CouponReceiveLog couponReceiveLog) {
        Long userid = couponReceiveLog.getUserId();
        Long voucherId = couponReceiveLog.getVoucherId();
        Long orderId = couponReceiveLog.getId();


        RLock lock = redissonClient.getLock("seckill:voucher:" + voucherId);

        try{
            boolean isLock  = lock.tryLock(30, TimeUnit.SECONDS);
            if (!isLock){
                log.info("获取Redisson锁失败，请重试!");
                throw new RuntimeException("系统繁忙，请稍后再试！");
            }
            long count = couponReceiveLogService.count(new LambdaQueryWrapper<CouponReceiveLog>()
                    .eq(CouponReceiveLog::getUserId, userid)
                    .eq(CouponReceiveLog::getVoucherId, voucherId));
            if (count>0){
                log.error("用户:{}已经秒杀过优惠券:{}", userid, voucherId);
                return Result.fail("失败");
            }
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)
                    .update();
            if (!success){
                log.error("秒杀券:{}库存不足", voucherId);
                throw new RuntimeException("库存不足");
            }
            couponReceiveLogService.save(couponReceiveLog);
            log.info("用户:{}秒杀成功，订单号:{}", userid, orderId);
        }catch (InterruptedException e){
            log.error("获取锁失败, InterruptedException:", e);
            throw new RuntimeException("系统错误请稍后再试！");
        }finally {
            try{
                lock.unlock();
            }catch (Exception e){
                log.error("Redisson unlock error", e);
            }
        }
        return Result.ok("下单成功");
    }

}

