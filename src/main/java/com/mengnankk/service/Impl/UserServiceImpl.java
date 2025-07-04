package com.mengnankk.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mengnankk.dto.TokenResponse;
import com.mengnankk.entity.Role;
import com.mengnankk.entity.User;
import com.mengnankk.exception.AuthException;
import com.mengnankk.exception.ResourceNotFoundException;
import com.mengnankk.mapper.RoleMapper;
import com.mengnankk.mapper.UserMapper;
import com.mengnankk.service.BloomFilterService;
import com.mengnankk.service.UserService;
import com.mengnankk.utils.JwtTokenUntils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUntils jwtUtils;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AliyunSmsService aliyunSmsService;

    @Autowired
    private BloomFilterService userBloomFilter; // 用户ID布隆过滤器

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:"; // Key: refresh_token:<userId> Value: <refreshTokenString>
    private static final String ACCESS_TOKEN_BLACKLIST_KEY_PREFIX = "blacklist:at:"; // Key: blacklist:at:<jti> Value: "true"
    private static final String REFRESH_TOKEN_BLACKLIST_KEY_PREFIX = "blacklist:rt:"; // Key: blacklist:rt:<jti> Value: "true"


    /**
     * 注册新用户
     * @param username
     * @param phoneNumber
     * @param password
     * @param smsCode
     * @return
     */
    @Override
    @Transactional
    public User register(String username, String phoneNumber, String password, String smsCode) {
        aliyunSmsService.verifyRegisterSmsCode(phoneNumber,smsCode);
        if (userMapper.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userMapper.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("手机号已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(passwordEncoder.encode(password)); // Bcrypt加密密码
        user.setStatus("ACTIVE"); // 默认激活状态
        user.setRegistrationTime(new Timestamp(System.currentTimeMillis()));

        userMapper.insertUser(user);
        log.info("Registered new user: {}", user.getUsername());

        Role defaultRole = roleMapper.findByName("USER");
        if (defaultRole==null){
            log.warn("Default role 'USER' not found, please ensure it's in the database.");
            throw new ResourceNotFoundException("默认角色未配置");
        }
        userMapper.insertUserRole(user.getId(),defaultRole.getId());
        log.info("Assigned default role 'USER' to user ID: {}", user.getId());

        userBloomFilter.put(user.getId());
        log.info("User ID {} added to Bloom Filter.", user.getId());
        return user;

    }

    /**
     * 登出
     * @param userId
     */

    @Override
    public void logout(Long userId) {
        String redisKey = REFRESH_TOKEN_KEY_PREFIX+userId;
        Object rt =redisTemplate.opsForValue().get(redisKey);
        if (rt!=null){
            try {
                Claims claims = jwtUtils.getClaimsFromToken(rt.toString());
                blacklistToken(claims.getId(), claims.getExpiration().getTime());
            } catch (Exception e) {
                log.error("Failed to parse Refresh Token during logout for user {}: {}", userId, e.getMessage());
            }
            redisTemplate.delete(redisKey);
            log.info("User {} logged out, Refresh Token removed from Redis.", userId);

        }
    }

    @Override
    public User getUserDetails(Long userId) {
        if (!userBloomFilter.mightContain(userId)) {
            log.warn("User ID {} not found in Bloom Filter. Possible invalid ID or non-existent user (cache penetration attempt).", userId);
            throw new ResourceNotFoundException("该用户不存在(Bloom Filter)");
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            log.warn("User ID {} found in Bloom Filter but not in DB. Bloom filter false positive or DB inconsistency.", userId);
            throw new ResourceNotFoundException("用户不存在");
        }
        user.setRoles(roleMapper.findRolesByUserId(userId));
        return user;
    }

    /**
     * 将 Token 加入黑名单
     * @param jti
     * @param expirationTimeMillis
     */
    @Override
    public void blacklistToken(String jti, long expirationTimeMillis) {
        String blacklistKey = ACCESS_TOKEN_BLACKLIST_KEY_PREFIX+jti;
        long ttl =expirationTimeMillis-System.currentTimeMillis();
        if (ttl>0){
            redisTemplate.opsForValue().set(blacklistKey,"true",ttl, TimeUnit.SECONDS);
            log.info("Token with JTI {} blacklisted for {} ms.", jti, ttl);
        }else {
            log.warn("Attempted to blacklist an already expired token with JTI {}.", jti);
        }
    }

    /**
     * 是不是在黑名单中
     * @param jti
     * @return
     */

    @Override
    public boolean isTokenBlacklisted(String jti) {
        String blacklistKey = ACCESS_TOKEN_BLACKLIST_KEY_PREFIX + jti;
        Boolean exists = redisTemplate.hasKey(blacklistKey);
        log.debug("Checking blacklist for JTI {}: {}", jti, exists);
        return exists!=null&&exists;
    }

    /**
     * 登录
     * @param identifier
     * @param password
     * @return
     */
    @Override
    public TokenResponse login(String identifier, String password) {
        User user;
        if (identifier.matches("^1[3-9]\\d{9}$")){
            user =userMapper.findByPhoneNumber(identifier);
        }else {
            user = userMapper.findByUsername(identifier);
        }

        Optional.ofNullable(user)
                .orElseThrow(()->new AuthException("\"用户名/手机号或密码不正确\""));
        if (!passwordEncoder.matches(password,user.getPassword())){
            throw new AuthException("用户名/手机号或密码不正确");
        }
        if ("DiSABLED".equals(user.getStatus())||"LOCKED".equals(user.getStatus())){
            throw new AuthException("账户状态异常，请联系管理员。");
        }
        List<String> roles =userMapper.findUserRoles(user.getId());
        List<String> permissions = userMapper.findUserPermissions(user.getId());

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), roles, permissions);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        String redisKey = REFRESH_TOKEN_KEY_PREFIX+user.getId();
        redisTemplate.opsForValue().set(redisKey,refreshToken,refreshTokenExpirationMs,TimeUnit.SECONDS);
        log.info("User {} logged in, Refresh Token stored in Redis. Key: {}", user.getId(), redisKey);
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtUtils.getAccessTokenExpirationMs());
    }
}
