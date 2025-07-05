package com.mengnankk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_voucher") // 优惠券表
public class Coupon {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String subTitle;
    private String rules;
    private Long payValue;
    private Long actualValue;
    private Integer type;
    private boolean valid;
    @TableField(exist = false)
    private  Integer stock; //库存
}
