package org.indoles.apigatewayserver.util;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "member-service", url = "http://localhost:7070")
public interface RefreshTokenClient {

    @PostMapping("/members/refresh")
    ResponseEntity<SignInResponseInfo> refreshAccessToken(@RequestBody RefreshTokenRequest request);
}
