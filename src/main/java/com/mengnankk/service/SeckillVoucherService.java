package com.mengnankk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengnankk.entity.SeckillVoucher;

public interface SeckillVoucherService extends IService<SeckillVoucher> {
    SeckillVoucher  getSeckillVoucherByVoucherId(Long voucherId);
}
