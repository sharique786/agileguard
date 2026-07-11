package com.agileguard.jira.controller;

import com.agileguard.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proxy controller that fetches Confluence page content on behalf of the frontend.
 *
 * Why a proxy?
 *   Browsers cannot call Confluence REST API directly due to CORS.
 *   This controller forwards the request server-side using the tenant's
 *   Confluence credentials stored in auth-service.
 *
 * Endpoint:
 *   POST /api/confluence/fetch
 *   Body: { "url": "https://acme.atlassian.net/wiki/spaces/COMMSSURV/pages/12345/Feature+Spec" }
 *
 * Returns ConfluencePageContent with:
 *   - pageId, title, spaceKey
 *   - bodyText  — plain text stripped of all HTML/wiki markup
 *   - excerpt   — first 500 chars for UI preview
 *
 * Supported URL patterns:
 *   /wiki/spaces/{space}/pages/{pageId}/{title}     (modern Confluence Cloud)
 *   /wiki/pages/viewpage.action?pageId={pageId}     (legacy/server)
 *   /wiki/pages/editpage.action?pageId={pageId}     (edit mode — same page)
 */
@RestController
@RequestMapping("/api/confluence")
@RequiredArgsConstructor
@Slf4j
public class ConfluenceController {

    @Value("${agileguard.confluence.use-mock:true}")
    private boolean useMock;

    @Value("${agileguard.confluence.base-url:}")
    private String defaultBaseUrl;

    @Value("${agileguard.confluence.api-token:}")
    private String defaultApiToken;

    @Value("${agileguard.confluence.user-email:}")
    private String defaultUserEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── Regex patterns for Confluence URL parsing ──────────────────────────
    private static final Pattern MODERN_URL = Pattern.compile(
            "https://([^/]+)/wiki/spaces/([^/]+)/pages/(\\d+)");
    private static final Pattern LEGACY_URL = Pattern.compile(
            "https://([^/]+)/wiki/.*pageId=(\\d+)");

    @PostMapping("/fetch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fetchPage(
            @RequestBody Map<String, String> body) {

        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Confluence URL is required"));
        }

        log.info("Fetching Confluence page: {}", url);

        if (useMock) {
            return ResponseEntity.ok(ApiResponse.success(
                    "Confluence page fetched (mock)", buildMockPage(url)));
        }

        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Confluence page fetched", fetchRealPage(url)));
        } catch (Exception e) {
            log.error("Failed to fetch Confluence page {}: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    ApiResponse.error("Failed to fetch Confluence page: " + e.getMessage()));
        }
    }

    // ── Real fetch ─────────────────────────────────────────────────────────

    private Map<String, Object> fetchRealPage(String pageUrl) throws Exception {
        // 1. Parse the pageId and site from the URL
        String siteHost = parseSiteHost(pageUrl);
        String pageId   = parsePageId(pageUrl);

        if (pageId == null) {
            throw new IllegalArgumentException(
                    "Cannot extract pageId from URL. Supported: /wiki/spaces/X/pages/{id}/... or ?pageId={id}");
        }

        // 2. Build Confluence REST API v2 call
        String apiUrl = String.format("https://%s/wiki/rest/api/content/%s?expand=body.view,space,metadata.properties",
                siteHost, pageId);

        // 3. Call the API with Basic Auth (email:token)
        HttpHeaders headers = new HttpHeaders();
        String credentials = defaultUserEmail + ":" + defaultApiToken;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);
        headers.set("Accept", "application/json");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, request, Map.class);
        Map<?, ?> data = response.getBody();

        if (data == null) throw new RuntimeException("Empty response from Confluence");

        // 4. Extract fields
        String title    = String.valueOf(data.get("title"));
        Map<?, ?> space = (Map<?, ?>) data.get("space");
        String spaceKey = space != null ? String.valueOf(space.get("key")) : "";
        Map<?, ?> bodyMap = (Map<?, ?>) data.get("body");
        Map<?, ?> view    = bodyMap != null ? (Map<?, ?>) bodyMap.get("view") : null;
        String htmlBody   = view != null ? String.valueOf(view.get("value")) : "";

        // 5. Strip HTML to plain text using Jsoup
        Document doc = Jsoup.parse(htmlBody);
        String plainText = doc.text();
        String excerpt   = plainText.length() > 500 ? plainText.substring(0, 500) + "…" : plainText;

        return Map.of(
                "pageId",    pageId,
                "title",     title,
                "spaceKey",  spaceKey,
                "url",       pageUrl,
                "bodyText",  plainText,
                "excerpt",   excerpt,
                "fetchedAt", Instant.now().toString()
        );
    }

    // ── Mock page for local development ────────────────────────────────────

    private Map<String, Object> buildMockPage(String url) {
        String title = "Feature Specification — Payments Gateway v2";
        String body  = """
            Feature Specification: Payments Gateway v2

            Overview:
            This document describes the requirements for the Payments Gateway v2 project.
            The goal is to enable multi-currency support for all merchant accounts, allowing
            transactions in USD, GBP, EUR, AUD, and CAD without requiring separate merchant
            accounts per currency.

            Business Context:
            Currently 34% of international checkout attempts fail due to currency mismatch.
            Merchants in the UK market report losing an estimated £2.3M annually to this issue.

            Key Requirements:
            1. Currency detection — automatically detect customer's preferred currency from
               browser locale and billing address country.
            2. Real-time FX conversion — integrate with ECB and Open Exchange Rates API to
               apply live exchange rates at checkout time.
            3. Settlement reporting — provide daily settlement reports in merchant's base currency.
            4. Dispute handling — multi-currency disputes must preserve original transaction
               currency for chargebacks.
            5. PCI DSS compliance — all currency conversion logic must run server-side.

            Non-Functional Requirements:
            - Latency: currency conversion API response < 150ms P95
            - Availability: 99.95% uptime SLA
            - Audit: all FX conversions logged with timestamp, source rate, and provider

            Out of Scope for v2:
            - Cryptocurrency support
            - Dynamic currency conversion (DCC) at point of sale
            """;
        String excerpt = body.substring(0, Math.min(500, body.length())) + "…";
        return Map.of(
                "pageId",    "12345",
                "title",     title,
                "spaceKey",  "COMMSSURV",
                "url",       url,
                "bodyText",  body,
                "excerpt",   excerpt,
                "fetchedAt", Instant.now().toString()
        );
    }

    // ── URL parsing helpers ────────────────────────────────────────────────

    private String parseSiteHost(String url) {
        Matcher m = MODERN_URL.matcher(url);
        if (m.find()) return m.group(1);
        m = LEGACY_URL.matcher(url);
        if (m.find()) return m.group(1);
        try { return new URI(url).getHost(); } catch (Exception e) { return ""; }
    }

    private String parsePageId(String url) {
        Matcher m = MODERN_URL.matcher(url);
        if (m.find()) return m.group(3);
        m = LEGACY_URL.matcher(url);
        if (m.find()) return m.group(2);
        return null;
    }
}
