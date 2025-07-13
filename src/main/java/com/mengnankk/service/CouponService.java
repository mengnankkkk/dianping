package com.mengnankk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengnankk.dto.Result;
import com.mengnankk.entity.Coupon;
import com.mengnankk.entity.CouponReceiveLog;
import com.mengnankk.mapper.CouponMapper;

public interface CouponService extends IService<Coupon> {
    public Result seckillVoucher(Long voucherId);
   public Result createVoucherOrder(CouponReceiveLog couponReceiveLog);

}
