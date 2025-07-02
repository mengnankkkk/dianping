package com.mengnankk.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.List;


@Data
public class Role {
    @TableId
    private Long id;
    private String roleName;
    private List<Permission> permissions; // 角色拥有的权限列表
}
