package org.indoles.apigatewayserver.filter;

import lombok.extern.slf4j.Slf4j;
import org.indoles.apigatewayserver.exception.*;
import org.indoles.apigatewayserver.util.JwtTokenProvider;
import org.indoles.apigatewayserver.util.RefreshTokenClient;
import org.indoles.apigatewayserver.util.RefreshTokenRequest;
import org.indoles.apigatewayserver.util.SignInResponseInfo;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CustomGlobalFilter extends AbstractGatewayFilterFactory<CustomGlobalFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenClient refreshTokenClient;

    public CustomGlobalFilter(JwtTokenProvider jwtTokenProvider, @Lazy RefreshTokenClient refreshTokenClient) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenClient = refreshTokenClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            log.info("Request Path: {}", path);

            if (path.startsWith("/members/signup") || path.startsWith("/members/signin") || path.startsWith("/auctions")) {
                return chain.filter(exchange);
            }

            String authorizationHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String accessToken = authorizationHeader.substring(7);

                if (jwtTokenProvider.validateToken(accessToken)) {
                    return chain.filter(exchange);
                } else {
                    String refreshToken = exchange.getRequest().getHeaders().getFirst("Refresh-Token");
                    if (refreshToken != null) {
                        return refreshAccessToken(refreshToken)
                                .flatMap(newAccessToken -> {
                                    var mutatedRequest = exchange.getRequest().mutate()
                                            .header("Authorization", "Bearer " + newAccessToken)
                                            .build();
                                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                                });
                    }
                }
            }

            log.error("Unauthorized: No valid access token or refresh token found.");
            return Mono.error(new AuthenticationException("Unauthorized: No valid access token or refresh token found.", ErrorCode.AU04));
        };
    }

    private Mono<String> refreshAccessToken(String refreshToken) {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
        ResponseEntity<SignInResponseInfo> responseEntity = refreshTokenClient.refreshAccessToken(request);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return Mono.just(responseEntity.getBody().accessToken()); // 새로운 액세스 토큰 반환
        }
        return Mono.error(new RuntimeException("Failed to refresh access token."));
    }

    public static class Config {
    }
}



