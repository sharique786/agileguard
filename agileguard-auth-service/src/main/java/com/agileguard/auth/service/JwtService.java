package com.agileguard.auth.service;

import com.agileguard.auth.entity.AppUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for generating, parsing, and validating JWT tokens.
 * Access tokens expire after 15 minutes; refresh tokens after 7 days.
 */
@Service
@Slf4j
public class JwtService {

    @Value("${agileguard.jwt.secret}")
    private String secretKey;

    @Value("${agileguard.jwt.access-token-expiry-ms:900000}")
    private long accessTokenExpiryMs;   // default 15 min

    @Value("${agileguard.jwt.refresh-token-expiry-ms:604800000}")
    private long refreshTokenExpiryMs;  // default 7 days

    /**
     * Generates a JWT access token embedding tenant, role, and feature-team claims.
     *
     * @param user the authenticated user
     * @return signed JWT string
     */
    public String generateAccessToken(AppUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tenantId", user.getTenant().getId());
        claims.put("role", user.getRole().name());
        claims.put("fullName", user.getFullName());
        if (user.getFeatureTeam() != null) {
            claims.put("featureTeamId", user.getFeatureTeam().getId());
            claims.put("projectId", user.getFeatureTeam().getProjectTeam().getId());
        }
        return buildToken(claims, user.getEmail(), accessTokenExpiryMs);
    }

    /**
     * Generates a refresh token with minimal claims.
     *
     * @param user the authenticated user
     * @return signed JWT refresh token string
     */
    public String generateRefreshToken(AppUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("tenantId", user.getTenant().getId());
        return buildToken(claims, user.getEmail(), refreshTokenExpiryMs);
    }

    /** Builds and signs a JWT with the given claims and expiry. */
    private String buildToken(Map<String, Object> claims, String subject, long expiryMs) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /** Extracts the email (subject) from a JWT. */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extracts the tenant ID claim from a JWT. */
    public String extractTenantId(String token) {
        return extractClaim(token, claims -> claims.get("tenantId", String.class));
    }

    /** Validates a JWT against an expected user. */
    public boolean isTokenValid(String token, String email) {
        try {
            final String tokenEmail = extractEmail(token);
            return tokenEmail.equals(email) && !isTokenExpired(token);
        } catch (JwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    /** Returns true if the JWT's expiry date is in the past. */
    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /** Generic claim extractor using a resolver function. */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
