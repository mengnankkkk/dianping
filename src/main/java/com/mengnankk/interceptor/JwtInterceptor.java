package com.mengnankk.interceptor;


import com.mengnankk.entity.User;
import com.mengnankk.mapper.UserMapper;
import com.mengnankk.service.UserService;
import com.mengnankk.utils.AuthContextHolder;
import com.mengnankk.utils.JwtTokenUntils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenUntils jwtTokenUtils;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String ACCESS_TOKEN_BLACKLIST_KEY_PREFIX = "blacklist:at:";
    private static final String REFRESH_TOKEN_BLACKLIST_KEY_PREFIX = "blacklist:rt:";


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String accesstoken = request.getHeader("Authorization");
        String refreshToken = request.getHeader("X-Refresh-Token");


        if (accesstoken == null || !accesstoken.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for URI: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.getWriter().write("{\"code\":\"401\", \"message\":\"缺少或无效的Access Token\"}");
            return false;
        }

        String accessToken = accesstoken.substring(7);
        Claims claims;

        try {
            claims = jwtTokenUtils.getClaimsFromToken(accessToken);
            // 检查 Access Token 是否在黑名单
            if (userService.isTokenBlacklisted(claims.getId())) {
                log.warn("Access Token (JTI: {}) is blacklisted for URI: {}", claims.getId(), request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"code\":\"401\", \"message\":\"Access Token 已被列入黑名单\"}");
                return false;
            }
        } catch (ExpiredJwtException e) {
            log.warn("Access Token expired for URI: {}", request.getRequestURI());
            return handleExpiredAccessToken(request, response, refreshToken, e);
        } catch (Exception e) {
            log.error("Invalid Access Token signature or other error for URI: {}. Error: {}", request.getRequestURI(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            response.getWriter().write("{\"code\":\"401\", \"message\":\"Access Token 无效或签名错误\"}");
            return false;
        }
        setAuthContext(claims);
        return true;
    }
    private boolean handleExpiredAccessToken(HttpServletRequest request, HttpServletResponse response, String refreshToken, ExpiredJwtException expiredJwtException) throws Exception{
        if (refreshToken==null||refreshToken.isEmpty()){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":\"401\", \"message\":\"Access Token 已过期且缺少 Refresh Token\"}");
            return false;
        }
        try {
            Claims rtCliam = jwtTokenUtils.getClaimsFromToken(refreshToken);
            Long userId = Long.parseLong(rtCliam.getSubject());
            String rtjti =rtCliam.getId();

            String storedRefreshToken = (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY_PREFIX+userId);
            if (storedRefreshToken==null||!storedRefreshToken.equals(refreshToken)){
                log.warn("Invalid or revoked Refresh Token for user ID: {}. URI: {}", userId, request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 Forbidden: Refresh Token 无效
                response.getWriter().write("{\"code\":\"403\", \"message\":\"Refresh Token 无效或已失效，请重新登录\"}");
                return false;
            }
            if (userService.isTokenBlacklisted(rtjti)){
                log.warn("Refresh Token (JTI: {}) is blacklisted for user ID: {}. URI: {}", rtjti, userId, request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"code\":\"403\", \"message\":\"Refresh Token 已被列入黑名单\"}");
                return false;
            }
            User user = userService.getUserDetails(userId);

            List<String> roleNames = user.getRoles().stream()
                    .map(role -> role.getRoleName())
                    .collect(Collectors.toList());

            List<String> permissions = userMapper.findUserPermissions(userId);

            String newAccessToken = jwtTokenUtils.generateAccessToken(user.getId(), user.getUsername(), roleNames, permissions);
            String newRefreshToken = jwtTokenUtils.generateRefreshToken(user.getId());

            redisTemplate.opsForValue().set(REFRESH_TOKEN_KEY_PREFIX+userId,newRefreshToken,jwtTokenUtils.getRefreshTokenExpirationMs(), TimeUnit.MICROSECONDS);


            try {
                userService.blacklistToken(expiredJwtException.getClaims().getId(), expiredJwtException.getClaims().getExpiration().getTime());
            } catch (Exception e) {
                log.warn("Could not blacklist old Access Token JTI: {}", expiredJwtException.getClaims().getId(), e);
            }
            userService.blacklistToken(rtjti,rtCliam.getExpiration().getTime());
            response.setHeader("X-New-Access-Token", newAccessToken);
            response.setHeader("X-New-Refresh-Token", newRefreshToken);
            response.setHeader("Access-Control-Expose-Headers", "X-New-Access-Token, X-New-Refresh-Token");

            setAuthContext(jwtTokenUtils.getClaimsFromToken(newAccessToken));
            log.info("Tokens refreshed for user {}. New tokens in response headers. URI: {}", userId, request.getRequestURI());
            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Refresh Token expired for user. URI: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
            response.getWriter().write("{\"code\":\"403\", \"message\":\"Refresh Token 也已过期，请重新登录\"}");
            return false;
        }catch (Exception e){
            log.error("Refresh Token validation failed or other error during refresh. URI: {}. Error: {}", request.getRequestURI(), e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
            response.getWriter().write("{\"code\":\"403\", \"message\":\"Refresh Token 无效或处理异常，请重新登录\"}");
            return false;
        }
    }



    private void setAuthContext(Claims claims) {
        AuthContextHolder.setUserId(Long.parseLong(claims.getSubject()));
        AuthContextHolder.setUsername(claims.get("username", String.class));
        AuthContextHolder.setRoles(claims.get("roles", List.class)); // 注意：这里可能需要List<String>转换
        AuthContextHolder.setPermissions(claims.get("permissions", List.class)); // 同上
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        AuthContextHolder.clear(); // 清理 ThreadLocal
    }
}
