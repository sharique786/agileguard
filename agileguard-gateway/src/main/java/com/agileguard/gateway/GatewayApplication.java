package com.agileguard.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgileGuard API Gateway.
 * Entry point for all client requests. Handles:
 *   - JWT token validation and propagation
 *   - Tenant-ID header injection
 *   - Request routing to microservices
 *   - CORS for Angular frontend
 * Runs on port 8080.
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
