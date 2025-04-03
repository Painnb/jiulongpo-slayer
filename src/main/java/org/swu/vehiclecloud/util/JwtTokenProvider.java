package org.swu.vehiclecloud.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // 根据用户id生成JWT token
    public String generateToken(Integer id) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        String StringId = id.toString();

        return Jwts.builder()
                .setSubject(StringId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    // 从token当中获取用户id
    public String getUserIdFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 捕获过期异常，但仍提取用户ID
            return e.getClaims().getSubject();
        }
    }

    // 验证token是否过期
    public boolean validateTokenExpiration(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();

            Date expirationDate = claims.getExpiration();
            return expirationDate.before(new Date()); // 如果当前时间在过期时间之后，返回true，即已过期
        } catch (Exception e) {
            return true;  // 如果解析异常，认为 token 已经过期
        }
    }
}

