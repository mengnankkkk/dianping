package com.mengnankk.dto;

import lombok.Data;
import org.aspectj.weaver.AjcMemberMaker;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class LoginRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phoneNumber;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "不为空")
    public String Identifier;

}
