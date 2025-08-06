package com.mengnankk.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenUntils {
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;
    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey key;

    /**
     * 初始化base64密钥
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    /**
     * 生成新的AccessToken
     *
     * @param userId
     * @param username
     * @param roles
     * @return
     */
    public String generateAccessToken(Long userId, String username, List<String> roles,List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(Long.toString(userId))
                .id(jti)
                .claim("username", username)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 生成AccessToken（带字符串参数）
     */
    public String generateAccessToken(String userId, String username, String roles, String permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(userId)
                .id(jti)
                .claim("username", username)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 生成refreshtoken
     *
     * @param userId
     * @return
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(Long.toString(userId))
                .id(jti)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 生成RefreshToken（带字符串参数）
     */
    public String generateRefreshToken(String userId, String username, String roles, String permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(userId)
                .id(jti)
                .claim("username", username)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 检查token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException | ExpiredJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            return false;
        }
    }
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
