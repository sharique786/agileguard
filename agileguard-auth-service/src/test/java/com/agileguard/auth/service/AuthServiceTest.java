package com.agileguard.auth.service;

import com.agileguard.auth.dto.LoginRequest;
import com.agileguard.auth.dto.RegisterRequest;
import com.agileguard.auth.dto.AuthResponse;
import com.agileguard.auth.entity.AppUser;
import com.agileguard.auth.entity.Tenant;
import com.agileguard.auth.repository.FeatureTeamRepository;
import com.agileguard.auth.repository.TenantRepository;
import com.agileguard.auth.repository.UserRepository;
import com.agileguard.common.enums.Role;
import com.agileguard.common.exception.AgileGuardException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * All external dependencies are mocked via Mockito.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private FeatureTeamRepository featureTeamRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;

    private Tenant mockTenant;
    private AppUser mockUser;

    @BeforeEach
    void setUp() {
        mockTenant = Tenant.builder()
                .id("tenant-001")
                .name("Deutsche Bank")
                .slug("db")
                .build();

        mockUser = AppUser.builder()
                .id("user-001")
                .email("dev@db.com")
                .fullName("Esha Basu")
                .password("encoded-password")
                .role(Role.DEVELOPER)
                .tenant(mockTenant)
                .build();
    }

    @Test
    @DisplayName("Register: Should create user and return JWT tokens for valid request")
    void register_validRequest_returnsAuthResponse() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@db.com");
        req.setFullName("New User");
        req.setPassword("Pass@1234");
        req.setTenantId("tenant-001");
        req.setRole(Role.DEVELOPER);

        when(userRepository.existsByEmail("new@db.com")).thenReturn(false);
        when(tenantRepository.findById("tenant-001")).thenReturn(Optional.of(mockTenant));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-pass");
        when(userRepository.save(any())).thenReturn(mockUser);
        when(jwtService.generateAccessToken(any())).thenReturn("access.token.jwt");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh.token.jwt");

        AuthResponse response = authService.register(req);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token.jwt");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token.jwt");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(AppUser.class));
    }

    @Test
    @DisplayName("Register: Should throw conflict when email already exists")
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@db.com");
        req.setTenantId("tenant-001");

        when(userRepository.existsByEmail("existing@db.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(AgileGuardException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register: Should throw 404 when tenant not found")
    void register_tenantNotFound_throwsNotFound() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@test.com");
        req.setTenantId("bad-tenant-id");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantRepository.findById("bad-tenant-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(AgileGuardException.class)
                .hasMessageContaining("Tenant not found");
    }

    @Test
    @DisplayName("Login: Should return tokens for valid credentials")
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest req = new LoginRequest();
        req.setEmail("dev@db.com");
        req.setPassword("Dev@1234");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("dev@db.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateAccessToken(mockUser)).thenReturn("access.token");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("refresh.token");

        AuthResponse response = authService.login(req);

        assertThat(response.getEmail()).isEqualTo("dev@db.com");
        assertThat(response.getRole()).isEqualTo(Role.DEVELOPER);
        assertThat(response.getTenantId()).isEqualTo("tenant-001");
    }

    @Test
    @DisplayName("Login: Should propagate BadCredentialsException for wrong password")
    void login_wrongPassword_throwsBadCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("dev@db.com");
        req.setPassword("wrong");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Refresh: Should return new tokens for valid refresh token")
    void refresh_validToken_returnsNewTokens() {
        when(jwtService.extractEmail("valid-refresh-token")).thenReturn("dev@db.com");
        when(userRepository.findByEmail("dev@db.com")).thenReturn(Optional.of(mockUser));
        when(jwtService.isTokenValid("valid-refresh-token", "dev@db.com")).thenReturn(true);
        when(jwtService.generateAccessToken(mockUser)).thenReturn("new.access.token");
        when(jwtService.generateRefreshToken(mockUser)).thenReturn("new.refresh.token");

        AuthResponse response = authService.refresh("valid-refresh-token");

        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
    }
}
