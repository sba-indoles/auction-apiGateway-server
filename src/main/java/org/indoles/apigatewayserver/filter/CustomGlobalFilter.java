package org.indoles.apigatewayserver.filter;

import lombok.extern.slf4j.Slf4j;
import org.indoles.apigatewayserver.exception.*;
import org.indoles.apigatewayserver.service.RefreshTokenService;
import org.indoles.apigatewayserver.util.JwtTokenProvider;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class CustomGlobalFilter extends AbstractGatewayFilterFactory<CustomGlobalFilter.Config> {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public CustomGlobalFilter(JwtTokenProvider jwtTokenProvider, RefreshTokenService refreshTokenService) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            log.info("Request Path: {}", path);

            if (path.startsWith("/members/signup") || path.startsWith("/members/signin") || path.startsWith("/auctions/search") || path.startsWith("/auctions/{auctionId}")) {
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

                        return refreshTokenService.refreshAccessToken(refreshToken)
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

    public static class Config {
    }
}
