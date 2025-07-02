package com.mengnankk.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengnankk.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    User findById(Long id);
    User findByUsername(String username);
    User findByPhoneNumber(String phoneNumber);
    boolean existsByUsername(String username);
    boolean existsByPhoneNumber(String phoneNumber);
    void insertUser(User user);
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
    List<String> findUserRoles(Long userId);
    List<String> findUserPermissions(Long userId);
    void updateUserStatus(@Param("userId") Long userId, @Param("status") String status);
    void updatePassword(@Param("userId") Long userId, @Param("password") String password);

}
