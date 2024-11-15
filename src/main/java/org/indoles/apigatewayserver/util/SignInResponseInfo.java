package org.indoles.apigatewayserver.util;


public record SignInResponseInfo(
        Role role,
        String accessToken,
        String refreshToken
) {
}

