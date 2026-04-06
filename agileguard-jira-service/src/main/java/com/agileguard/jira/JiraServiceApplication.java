package com.agileguard.jira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AgileGuard JIRA Integration Service.
 * Connects to Atlassian JIRA REST API v3, detects quality gaps,
 * enforces mandatory fields, and guards status transitions.
 * Runs on port 8082.
 */
@SpringBootApplication
@EnableScheduling
public class JiraServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(JiraServiceApplication.class, args);
    }
}
