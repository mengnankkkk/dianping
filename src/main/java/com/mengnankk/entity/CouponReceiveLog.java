package com.mengnankk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_voucher_order")
public class CouponReceiveLog { //订单表

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 用户ID
    private String couponType;
    private LocalDateTime receiveTime;
    private   Long voucherId;
    private  Integer status; //0 未失效 1 已使用 2已过期
}
