package com.agileguard.auth.service;

import com.agileguard.auth.dto.AuthResponse;
import com.agileguard.auth.dto.LoginRequest;
import com.agileguard.auth.dto.RegisterRequest;
import com.agileguard.auth.entity.AppUser;
import com.agileguard.auth.entity.FeatureTeam;
import com.agileguard.auth.entity.Tenant;
import com.agileguard.auth.repository.FeatureTeamRepository;
import com.agileguard.auth.repository.TenantRepository;
import com.agileguard.auth.repository.UserRepository;
import com.agileguard.common.exception.AgileGuardException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration and login, returning JWT access + refresh tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final FeatureTeamRepository featureTeamRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user within an existing tenant.
     * Validates that the email is not already in use.
     *
     * @param request registration details
     * @return JWT tokens for the newly created user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AgileGuardException.conflict("Email already registered: " + request.getEmail());
        }

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> AgileGuardException.notFound("Tenant", request.getTenantId()));

        FeatureTeam featureTeam = null;
        if (request.getFeatureTeamId() != null) {
            featureTeam = featureTeamRepository.findById(request.getFeatureTeamId())
                    .orElseThrow(() -> AgileGuardException.notFound("FeatureTeam", request.getFeatureTeamId()));
        }

        AppUser user = AppUser.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .tenant(tenant)
                .featureTeam(featureTeam)
                .githubUsername(request.getGithubUsername())
                .jiraAccountId(request.getJiraAccountId())
                .build();

        userRepository.save(user);
        log.info("Registered new user: {} in tenant: {}", user.getEmail(), tenant.getName());

        return buildAuthResponse(user);
    }

    /**
     * Authenticates a user and returns JWT tokens.
     * Uses Spring Security's AuthenticationManager for credential validation.
     *
     * @param request login credentials
     * @return JWT tokens
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        AppUser user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> AgileGuardException.notFound("User", request.getEmail()));

        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * @param refreshToken the refresh JWT
     * @return new access token
     */
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> AgileGuardException.notFound("User", email));

        if (!jwtService.isTokenValid(refreshToken, email)) {
            throw AgileGuardException.badRequest("Invalid or expired refresh token");
        }

        return buildAuthResponse(user);
    }

    /** Builds the full AuthResponse DTO with both tokens. */
    private AuthResponse buildAuthResponse(AppUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .tenantId(user.getTenant().getId())
                .featureTeamId(user.getFeatureTeam() != null ? user.getFeatureTeam().getId() : null)
                .build();
    }
}
