package org.indoles.apigatewayserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.indoles.apigatewayserver.util.JwtTokenProvider;
import org.indoles.apigatewayserver.util.SignInInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RefreshTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    @Autowired
    public RefreshTokenService(JwtTokenProvider jwtTokenProvider, RedisService redisService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisService = redisService;
    }

    public Mono<String> refreshAccessToken(String refreshToken) {
        String userId = redisService.getUserIdFromRefreshToken(refreshToken);
        if (userId != null) {
            try {
                Long userIdLong = Long.valueOf(userId);

                String signInfoJson = jwtTokenProvider.getSignInInfoFromToken(refreshToken);
                SignInInfo signInInfo = new ObjectMapper().readValue(signInfoJson, SignInInfo.class);

                String newAccessToken = jwtTokenProvider.createAccessToken(signInInfo);
                String newRefreshToken = jwtTokenProvider.createRefreshToken(userIdLong, signInInfo.role());

                redisService.saveRefreshToken(newRefreshToken, userId, jwtTokenProvider.getExpiration() * 2);
                return Mono.just(newAccessToken);
            } catch (NumberFormatException e) {
                return Mono.error(new RuntimeException("Invalid user ID format."));
            } catch (JsonProcessingException e) {
                return Mono.error(new RuntimeException("Error processing SignInInfo JSON: " + e.getMessage()));
            } catch (Exception e) {
                return Mono.error(new RuntimeException("Error generating new tokens: " + e.getMessage()));
            }
        }
        return Mono.error(new RuntimeException("Invalid refresh token."));
    }
}


