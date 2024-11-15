package org.indoles.apigatewayserver.util;

import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.indoles.apigatewayserver.exception.SignInInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.indoles.apigatewayserver.util.Role;


@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;


    @PostConstruct
    protected void init() {
        log.debug("Encoded secret key: {}", secretKey);
    }

    public SignInInfo getSignInInfoFromToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(secretKey.getBytes()).parseClaimsJws(token).getBody();
            Long userId = Long.valueOf(claims.getSubject());
            String roleStr = claims.get("role", String.class);
            Role role = Role.valueOf(roleStr);
            log.debug("Extracted userId: {}, role: {}", userId, roleStr);
            return new SignInInfo(userId, role);
        } catch (Exception e) {
            log.error("Error extracting SignInInfo from token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey.getBytes()).parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            log.error("JWT signature verification failed: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT token is malformed: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT token is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }
}
