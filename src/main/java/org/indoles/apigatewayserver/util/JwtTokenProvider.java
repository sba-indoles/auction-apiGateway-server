package org.indoles.apigatewayserver.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expiration;

    private final RedisTemplate<String, String> redisTemplate;

    public JwtTokenProvider(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    protected void init() {
        log.debug("Encoded secret key: {}", secretKey);
    }

    public String createAccessToken(SignInInfo signInfoRequest) {
        Claims claims = Jwts.claims().setSubject(signInfoRequest.id().toString());
        claims.put("role", signInfoRequest.role().name());
        Date now = new Date();

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration))
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
                .compact();
        log.debug("Generated Access Token: {}", token);
        return token;
    }

    public String createRefreshToken(Long userId, Role role) {
        Claims claims = Jwts.claims().setSubject(userId.toString());
        claims.put("role", role.name());

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String signInfoJson = objectMapper.writeValueAsString(new SignInInfo(userId, role));
            claims.put("signInInfo", signInfoJson);
        } catch (JsonProcessingException e) {
            log.error("Error converting SignInfoRequest to JSON: {}", e.getMessage());
        }

        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 2))
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes())
                .compact();

        redisTemplate.opsForValue().set(refreshToken, userId.toString(), expiration * 2, TimeUnit.MILLISECONDS);
        return refreshToken;
    }

    public String getSignInInfoFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(secretKey.getBytes()).parseClaimsJws(token).getBody();
        return claims.get("signInInfo", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey.getBytes()).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    public long getExpiration() {
        return expiration;
    }
}

