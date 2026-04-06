package com.agileguard.auth.controller;

import com.agileguard.auth.dto.LoginRequest;
import com.agileguard.auth.dto.RegisterRequest;
import com.agileguard.auth.repository.TenantRepository;
import com.agileguard.auth.repository.UserRepository;
import com.agileguard.auth.entity.Tenant;
import com.agileguard.common.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.agileguard.auth.entity.AppUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for AuthController using an in-memory H2 database.
 * Tests the full HTTP request/response cycle including Spring Security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static Tenant testTenant;

    @BeforeEach
    void setUp() {
        if (testTenant == null) {
            testTenant = tenantRepository.save(Tenant.builder()
                    .name("Test Corp").slug("test-corp").build());
        }
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/auth/register - should register new user and return 201")
    void register_validRequest_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test-new@corp.com");
        req.setFullName("Test User");
        req.setPassword("TestPass@1234");
        req.setTenantId(testTenant.getId());
        req.setRole(Role.DEVELOPER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/auth/register - should return 409 for duplicate email")
    void register_duplicateEmail_returns409() throws Exception {
        // Save user first
        userRepository.save(AppUser.builder()
                .email("dup@corp.com").fullName("Dup").password(passwordEncoder.encode("P@ss1234"))
                .role(Role.DEVELOPER).tenant(testTenant).build());

        RegisterRequest req = new RegisterRequest();
        req.setEmail("dup@corp.com");
        req.setFullName("Dup Again");
        req.setPassword("P@ss1234");
        req.setTenantId(testTenant.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Email already registered")));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/auth/login - should return 200 with tokens for valid credentials")
    void login_validCredentials_returns200() throws Exception {
        // Create user
        userRepository.save(AppUser.builder()
                .email("login@corp.com").fullName("Login User")
                .password(passwordEncoder.encode("Login@1234"))
                .role(Role.DEVELOPER).tenant(testTenant).build());

        LoginRequest req = new LoginRequest();
        req.setEmail("login@corp.com");
        req.setPassword("Login@1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("login@corp.com"));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/auth/login - should return 401 for wrong password")
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("login@corp.com");
        req.setPassword("WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/register - should return 400 for missing required fields")
    void register_missingFields_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest(); // empty

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}
