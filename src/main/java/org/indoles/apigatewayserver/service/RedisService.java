package org.indoles.apigatewayserver.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveRefreshToken(String refreshToken, String userId, long expiration) {
        redisTemplate.opsForValue().set(refreshToken, userId, expiration, TimeUnit.MILLISECONDS);
    }

    public String getUserIdFromRefreshToken(String refreshToken) {
        return redisTemplate.opsForValue().get(refreshToken);
    }
}
