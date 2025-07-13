package com.mengnankk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_seckill_voucher")  // 使用 MyBatis Plus
public class SeckillVoucher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long voucherId; // 优惠券ID (关联 tb_voucher)
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    @TableField(exist = false)
    private Integer stock;
}