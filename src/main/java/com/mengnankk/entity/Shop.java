package com.mengnankk.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("tb_shop")
public class Shop {
    private Long id;                 // 主键
    private String name;            // 商铺名称
    private Long typeId;            // 商铺类型ID
    private String images;          // 商铺图片，多个图片以','分隔
    private String area;            // 商圈，如：陆家嘴
    private String address;         // 地址
    private Double x;               // 经度
    private Double y;               // 纬度
    private Long avgPrice;          // 均价，整数
    private Integer sold;           // 销量，ZEROFILL 表示展示时左补0，与 Java 无关
    private Integer comments;       // 评论数量
    private Integer score;          // 评分，乘以10保存的，例如4.7分保存为47
    private String openHours;       // 营业时间
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Object typeName;

    public Object getTypeName() {
        return typeName;

    }

    public void setTypeName(Object typeName) {
        this.typeName = typeName;
    }
}

