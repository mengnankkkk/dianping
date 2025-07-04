package com.mengnankk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;@Data
@TableName("tb_shop_type")
public class ShopType {
    private Long id;               // 主键
    private String name;           // 类型名称
    private String icon;           // 图标路径
    private Integer sort;          // 顺序
    private LocalDateTime createTime;  // 创建时间
    private LocalDateTime updateTime;  // 更新时间
}
