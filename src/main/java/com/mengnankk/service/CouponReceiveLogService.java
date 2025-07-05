package com.mengnankk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengnankk.entity.CouponReceiveLog;

public interface CouponReceiveLogService extends IService<CouponReceiveLog> {
    public boolean save (CouponReceiveLog vo );
    public boolean addSeckillTast ( CouponReceiveLog couponReceiveLog );

}
