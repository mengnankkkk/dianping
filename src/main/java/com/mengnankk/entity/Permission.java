package com.mengnankk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Permission {
    @TableId
    private Long id;
    private String name; // e.g., user:manage, sms:send
}