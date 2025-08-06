package com.mengnankk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.SeckillVoucher;

public interface SeckillVoucherService extends IService<SeckillVoucher> {
    SeckillVoucher getSeckillVoucherByVoucherId(Long voucherId);
    
    Result<?> addSeckillVoucher(SeckillVoucher seckillVoucher);
    
    Result<?> listSeckillVouchers();
    
    Result<?> querySeckillVoucher(Long voucherId);
}
