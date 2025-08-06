package com.mengnankk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mengnankk.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * User mapper interface
 */
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT COUNT(*) > 0 FROM tb_user WHERE username = #{username}")
    boolean existsByUsername(String username);
    
    @Select("SELECT COUNT(*) > 0 FROM tb_user WHERE phone_number = #{phoneNumber}")
    boolean existsByPhoneNumber(String phoneNumber);
    
    @Select("SELECT * FROM tb_user WHERE phone_number = #{phoneNumber}")
    User findByPhoneNumber(String phoneNumber);
    
    @Select("SELECT * FROM tb_user WHERE username = #{username}")
    User findByUsername(String username);
    
    @Insert("INSERT INTO tb_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    void insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
    
    @Select("SELECT r.name FROM tb_role r INNER JOIN tb_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> findUserRoles(Long userId);
    
    @Select("SELECT p.name FROM tb_permission p INNER JOIN tb_role_permission rp ON p.id = rp.permission_id INNER JOIN tb_user_role ur ON rp.role_id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> findUserPermissions(Long userId);
}
