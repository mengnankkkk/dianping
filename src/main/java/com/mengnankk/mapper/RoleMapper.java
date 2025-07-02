package com.mengnankk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengnankk.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    Role findByName(String name);
    List<Role> findAll();
    void insertRole(Role role);
    void insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
    List<Role> findRolesByUserId(Long userId);
}
