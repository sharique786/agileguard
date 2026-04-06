package com.agileguard.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgileGuard AI Service.
 * Integrates with Google Gemini API to validate JIRA stories,
 * verify acceptance criteria, and generate AI-powered suggestions.
 * Runs on port 8083.
 */
@SpringBootApplication
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
