package com.agileguard.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Global gateway filter that validates JWT on every request.
 * Public endpoints (auth, actuator) are skipped.
 * On success, injects X-Tenant-ID, X-User-ID, and X-Role headers
 * so downstream services can trust the request context.
 */
@Component
@Slf4j
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    @Value("${agileguard.jwt.secret}")
    private String secretKey;

    /** Paths that do not require JWT authentication. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Allow public endpoints through without JWT
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) return chain.filter(exchange);

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = parseToken(token);

            // Inject tenant and user context headers for downstream services
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.add("X-Tenant-ID", claims.get("tenantId", String.class));
                        headers.add("X-User-ID", claims.getSubject());
                        headers.add("X-Role", claims.get("role", String.class));
                        String featureTeamId = claims.get("featureTeamId", String.class);
                        if (featureTeamId != null) headers.add("X-Feature-Team-ID", featureTeamId);
                    }))
                    .build();

            return chain.filter(mutatedExchange);

        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (JwtException ex) {
            log.warn("Invalid JWT: {}", ex.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private Claims parseToken(String token) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    @Override
    public int getOrder() { return -1; } // Run before other filters
}
