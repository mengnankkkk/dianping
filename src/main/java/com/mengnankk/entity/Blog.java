package com.mengnankk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_blog")
public class Blog {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;

    private String name;
    private String icon;

    private String title;
    private String content;
    private Integer liked;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Boolean isLike;
    private Double distance;

}
