package com.mengnankk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mengnankk.dto.TokenResponse;
import com.mengnankk.entity.User;

public interface UserService extends IService<User> {
    User register(String username, String phoneNumber, String password, String smsCode);
    void logout(Long userId);

    User getUserDetails(Long userId);
    void blacklistToken(String jti, long expirationTimeMillis);
    boolean isTokenBlacklisted(String jti);
    public TokenResponse login(String identifier, String password);

}
