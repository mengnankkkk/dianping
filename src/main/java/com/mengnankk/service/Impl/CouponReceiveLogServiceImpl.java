package com.mengnankk.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mengnankk.entity.CouponReceiveLog;
import com.mengnankk.mapper.CouponReceiveLogMapper;
import com.mengnankk.service.CouponReceiveLogService;
import com.mengnankk.service.SeckillVoucherService;
import com.mengnankk.utils.AsyncDbRedisWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

@Service
public class CouponReceiveLogServiceImpl extends ServiceImpl<CouponReceiveLogMapper, CouponReceiveLog> implements CouponReceiveLogService {


    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SeckillVoucherService seckillVoucherService;
    private BlockingQueue<CouponReceiveLog> orderTasks= new ArrayBlockingQueue<>(1024*1024);




    @Override
    public boolean save(CouponReceiveLog vo) {
        try {
            this.baseMapper.insert(vo);
            return true;
        }catch (Exception e){
            throw new RuntimeException("增加失败");
        }
    }

    @Override
    public boolean addSeckillTast(CouponReceiveLog couponReceiveLog) {
        return orderTasks.add(couponReceiveLog);
    }
}
