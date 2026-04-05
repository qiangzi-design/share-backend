package com.share.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成、解析和验证JWT令牌
 */
@Component
public class JwtUtil {

    /**
     * JWT签名密钥
     * 从配置文件中读取，用于签名和验证令牌
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT令牌过期时间（秒）
     * 从配置文件中读取，用于设置令牌有效期
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 生成JWT令牌
     * @param username 用户名，作为令牌的主体(subject)
     * @return JWT令牌字符串
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", username);  // 设置主体为用户名
        claims.put("created", new Date());  // 设置创建时间
        return Jwts.builder()
                .setClaims(claims)  // 设置载荷
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))  // 设置过期时间
                .signWith(SignatureAlgorithm.HS512, secret)  // 使用HS512算法和密钥签名
                .compact();  // 压缩生成最终的JWT字符串
    }

    /**
     * 从JWT令牌中获取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();  // 从载荷中获取主体(subject)
    }

    /**
     * 从JWT令牌中获取过期时间
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();  // 从载荷中获取过期时间
    }

    /**
     * 验证JWT令牌是否有效
     * @param token JWT令牌
     * @return true: 令牌有效，false: 令牌无效
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);  // 解析令牌，验证签名
            return !isTokenExpired(token);  // 检查令牌是否过期
        } catch (Exception e) {
            return false;  // 解析异常或验证失败时返回false
        }
    }

    /**
     * 从JWT令牌中获取所有载荷信息
     * @param token JWT令牌
     * @return Claims对象，包含所有载荷信息
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)  // 设置签名密钥
                .parseClaimsJws(token)  // 解析JWT令牌
                .getBody();  // 获取载荷部分
    }

    /**
     * 检查令牌是否已过期
     * @param token JWT令牌
     * @return true: 已过期，false: 未过期
     */
    private boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());  // 比较过期时间和当前时间
    }
}