package com.agileguard.auth.controller;

import com.agileguard.auth.dto.JiraConnectionTestRequest;
import com.agileguard.auth.dto.JiraConnectionTestResponse;
import com.agileguard.auth.dto.OnboardingRequest;
import com.agileguard.auth.dto.OnboardingResponse;
import com.agileguard.auth.service.OnboardingService;
import com.agileguard.auth.service.TenantManagementService;
import com.agileguard.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the tenant onboarding workflow.
 *
 * Endpoints:
 *   POST /api/onboarding          — full one-shot onboarding (public, no auth)
 *   POST /api/onboarding/jira-test — validate JIRA credentials (public)
 *   GET  /api/onboarding/check-slug/{slug} — slug availability check (public)
 */
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService         onboardingService;
    private final TenantManagementService   mgmtService;

    /**
     * Executes the complete onboarding workflow in a single request.
     * No authentication required — this is the entry point for new tenants.
     *
     * Returns 201 Created with JWT tokens so the admin can navigate
     * directly to their new workspace without a separate login step.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> onboard(
            @Valid @RequestBody OnboardingRequest request) {
        OnboardingResponse result = onboardingService.onboard(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Onboarding complete. Welcome to AgileGuard!", result));
    }

    /**
     * Validates JIRA credentials before they are saved to the database.
     * Called from step 2 of the onboarding wizard's "Test Connection" button.
     */
    @PostMapping("/jira-test")
    public ResponseEntity<ApiResponse<JiraConnectionTestResponse>> testJiraConnection(
            @Valid @RequestBody JiraConnectionTestRequest request) {
        JiraConnectionTestResponse result = mgmtService.testJiraConnection(request);
        return ResponseEntity.ok(ApiResponse.success("JIRA connection tested", result));
    }

    /**
     * Returns whether a slug is available for use.
     * Called on keyup in the slug field of the onboarding wizard (debounced).
     */
    @GetMapping("/check-slug/{slug}")
    public ResponseEntity<ApiResponse<Boolean>> checkSlugAvailability(
            @PathVariable String slug) {
        boolean available = onboardingService.isSlugAvailable(slug);
        String msg = available ? "Slug is available" : "Slug is already taken";
        return ResponseEntity.ok(ApiResponse.success(msg, available));
    }
}
