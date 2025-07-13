package com.mengnankk.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tb_follow") // 对应数据库中的表名 (根据你的实际情况修改)
public class Follow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long followUserId;

    // ... 可以添加其他字段 ...
}
