package com.agileguard.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AgileGuard Authentication & Multi-Tenant Service.
 * Handles user registration, JWT issuance, tenant hierarchy, and RBAC.
 * Runs on port 8081.
 */
@SpringBootApplication
@EnableScheduling
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
