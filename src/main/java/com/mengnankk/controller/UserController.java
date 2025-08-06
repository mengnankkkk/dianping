package com.mengnankk.controller;


import com.mengnankk.dto.LoginRequest;
import com.mengnankk.dto.RegisterRequest;
import com.mengnankk.dto.SmsCodeRequest;
import com.mengnankk.dto.TokenResponse;
import com.mengnankk.entity.User;
import com.mengnankk.service.Impl.AliyunSmsService;
import com.mengnankk.service.UserService;
import com.mengnankk.utils.AuthContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AliyunSmsService aliyunSmsService;

    /**
     * 注册验证码
     * @param request
     * @return
     */
    @PostMapping("/send-register-sms-code")
    public ResponseEntity<String> sendRegisterSmsCode(@Valid @RequestBody SmsCodeRequest request){
        aliyunSmsService.sendRegisterSmsCode(request.getPhoneNumber());
        return ResponseEntity.ok("已发送");
    }
    /**
     * 发送找回密码短信验证码 (示例，需要实现找回密码逻辑)
     */
    @PostMapping("/send-reset-password-sms-code")
    public ResponseEntity<String> sendResetPasswordSmsCode(@Valid @RequestBody SmsCodeRequest request) {
        aliyunSmsService.sendResetPasswordSmsCode(request.getPhoneNumber());
        return ResponseEntity.ok("重置密码验证码已发送。");
    }
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request){
        User user = userService.register(request.getUsername(), request.getPhoneNumber(), request.getPassword(), request.getSmsCode());
        return new ResponseEntity<>("用户注册成功，用户ID: " + user.getId(), HttpStatus.CREATED);
    }
    /**
     * 用户登录 (支持用户名或手机号)
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = userService.login(request.getIdentifier(), request.getPassword());
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * 用户登出
     * @return
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            return new ResponseEntity<>("未登录或 Access Token 无效。", HttpStatus.UNAUTHORIZED);
        }
        userService.logout(userId);
        return ResponseEntity.ok("用户已成功登出。");
    }
}
