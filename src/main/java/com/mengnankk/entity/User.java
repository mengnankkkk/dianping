package com.mengnankk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
@TableName("tb_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phoneNumber;

    private String password;

    private Timestamp registrationTime;

    private List<Role> roles;

    private String username;

    private String status;

}