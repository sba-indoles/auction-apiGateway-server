package org.indoles.apigatewayserver.filter;

import org.indoles.apigatewayserver.exception.*;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class CustomGlobalFilter extends AbstractGatewayFilterFactory<CustomGlobalFilter.Config> {

    public CustomGlobalFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // 세션 가져오기
            return exchange.getSession().flatMap(session -> {
                // 로그인 및 회원가입 API 제외
                if (path.startsWith("/members/signup") || path.startsWith("/members/signin") ||
                        path.startsWith("/auctions")) {
                    return chain.filter(exchange);
                }

                SignInInfo signInInfo = (SignInInfo) session.getAttribute("signInMember");

                // 사용자 정보가 없을 경우
                if (signInInfo == null) {
                    return Mono.error(new AuthenticationException("세션에 사용자가 존재하지 않습니다.", ErrorCode.AU00));
                }

                // BuyerOnly 및 SellerOnly 경로에 대한 접근 제어
                if (path.startsWith("/auctions") && path.contains("/purchase")) {
                    if (!signInInfo.isType(Role.BUYER)) {
                        return Mono.error(new AuthorizationException("구매자만 요청할 수 있는 경로입니다.", ErrorCode.AU01));
                    }
                } else if (path.startsWith("/auctions") && (path.endsWith("/") || path.matches("/auctions/\\d+"))) {
                    // 판매자 전용 경로의 예시 (경매 등록, 취소 등)
                    if (!signInInfo.isType(Role.SELLER)) {
                        return Mono.error(new AuthorizationException("판매자만 요청할 수 있는 경로입니다.", ErrorCode.AU02));
                    }
                }

                // 요청에 SignInInfo 추가
                exchange.getRequest().mutate()
                        .header("X-SignIn-Info", signInInfo.toString())
                        .build();

                return chain.filter(exchange);
            });
        };
    }

    public static class Config {
    }
}


