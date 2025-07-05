package com.mengnankk.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mengnankk.entity.SeckillVoucher;
import com.mengnankk.mapper.SeckillVoucherMapper;
import com.mengnankk.service.SeckillVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillVoucherServiceImpl extends ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements SeckillVoucherService {

    @Autowired
    SeckillVoucherMapper seckillVoucherMapper;
    @Override
    public SeckillVoucher getSeckillVoucherByVoucherId(Long voucherId) {
        return seckillVoucherMapper.selectOne(new LambdaQueryWrapper<SeckillVoucher>().eq(SeckillVoucher::getVoucherId,voucherId));
    }
}
