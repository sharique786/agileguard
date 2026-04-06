package com.agileguard.github;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgileGuard GitHub Service.
 * Monitors GitHub Actions CI/CD runs, enforces SDLC gate checks,
 * and powers the developer leaderboard. Runs on port 8084.
 */
@SpringBootApplication
public class GitHubServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GitHubServiceApplication.class, args);
    }
}
