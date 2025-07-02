package com.mengnankk.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.security.Permission;
import java.util.List;


@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    Permission findByName(String name);
    List<Permission> findAll();
    void insertPermission(Permission permission);
}