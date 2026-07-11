package com.agileguard.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

/**
 * Per-route JWT validation filter factory for Spring Cloud Gateway.
 *
 * Naming convention REQUIRED by Spring Cloud Gateway:
 *   Class name must be "{FilterName}GatewayFilterFactory"
 *   YAML filter name must match "{FilterName}" exactly → "JwtValidation"
 *
 * This filter:
 *   1. Extracts the Bearer token from the Authorization header
 *   2. Validates the JWT signature and expiry using the shared secret
 *   3. On success: injects X-Tenant-ID, X-User-ID, X-Role headers for downstream services
 *   4. On failure: returns 401 Unauthorized immediately (request never reaches downstream)
 *
 * Usage in application.yml:
 *   filters:
 *     - JwtValidation
 *
 * Note: public routes (login, onboarding) should NOT include this filter.
 */
@Component
@Slf4j
public class JwtValidationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtValidationGatewayFilterFactory.Config> {

    @Value("${agileguard.jwt.secret}")
    private String jwtSecret;

    public JwtValidationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // ── 1. Check header presence ───────────────────────────────────
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("JwtValidation: missing or malformed Authorization header for {}",
                        exchange.getRequest().getPath());
                return reject(exchange, "Missing or malformed Authorization header");
            }

            String token = authHeader.substring(7);

            // ── 2. Validate JWT ────────────────────────────────────────────
            Claims claims;
            try {
                Key signingKey = Keys.hmacShaKeyFor(
                        Base64.getDecoder().decode(jwtSecret.getBytes(StandardCharsets.UTF_8)));
                claims = Jwts.parser()
                        .verifyWith((javax.crypto.SecretKey) signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();;

            } catch (ExpiredJwtException ex) {
                log.warn("JwtValidation: token expired for path {}", exchange.getRequest().getPath());
                return reject(exchange, "JWT token has expired");
            } catch (Exception ex) {
                log.warn("JwtValidation: invalid token for path {} — {}",
                        exchange.getRequest().getPath(), ex.getMessage());
                return reject(exchange, "Invalid JWT token");
            }

            // ── 3. Inject downstream headers ──────────────────────────────
            String tenantId = claims.get("tenantId", String.class);
            String userId   = claims.getSubject();
            String role     = claims.get("role", String.class);

            ServerWebExchange enrichedExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.set("X-Tenant-ID", tenantId != null ? tenantId : "");
                        headers.set("X-User-ID",   userId   != null ? userId   : "");
                        headers.set("X-Role",       role     != null ? role     : "");
                    }))
                    .build();

            return chain.filter(enrichedExchange);
        };
    }

    private Mono<Void> reject(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set("X-Auth-Error", reason);
        return exchange.getResponse().setComplete();
    }

    /** Config class required by AbstractGatewayFilterFactory (can be empty). */
    public static class Config {
        // No per-route configuration needed — JWT secret comes from application.yml
    }
}