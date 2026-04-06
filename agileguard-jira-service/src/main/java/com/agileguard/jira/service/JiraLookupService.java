package com.agileguard.jira.service;

import com.agileguard.jira.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides typeahead lookup endpoints for the Story Editor form:
 *   • Sprints       — active + future sprints for a project board
 *   • Components    — project components with names and descriptions
 *   • Fix Versions  — unreleased fix versions for a project
 *   • Users         — user search for the Reporter field
 *   • Epics         — resolve an epic key to its name/status
 *
 * All methods have a mock mode (use-mock: true) that returns realistic
 * sample data without requiring a real JIRA connection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraLookupService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${agileguard.jira.base-url:#{null}}")
    private String jiraBaseUrl;

    @Value("${agileguard.jira.user-email:#{null}}")
    private String jiraUserEmail;

    @Value("${agileguard.jira.api-token:#{null}}")
    private String jiraApiToken;

    @Value("${agileguard.jira.use-mock:true}")
    private boolean useMock;

    // ── Sprints ───────────────────────────────────────────────────────────────

    /**
     * Returns active and future sprints for a JIRA project.
     * Filters by optional query string (typeahead).
     *
     * JIRA API: GET /rest/agile/1.0/board/{boardId}/sprint?state=active,future
     */
    public List<JiraSprint> searchSprints(String projectKey, String query) {
        if (useMock) return filterMock(getMockSprints(projectKey), query,
                s -> s.getName().toLowerCase().contains(query.toLowerCase()));

        try {
            // In real mode: first resolve board ID from project key, then fetch sprints
            String url = jiraBaseUrl + "/rest/agile/1.0/board?projectKeyOrId=" + projectKey;
            String boardResp = buildClient().get().uri(url).retrieve().bodyToMono(String.class).block();
            JsonNode boardRoot = objectMapper.readTree(boardResp);
            String boardId = boardRoot.at("/values/0/id").asText();

            String sprintUrl = jiraBaseUrl + "/rest/agile/1.0/board/" + boardId
                    + "/sprint?state=active,future&maxResults=50";
            String sprintResp = buildClient().get().uri(sprintUrl).retrieve().bodyToMono(String.class).block();
            return parseSprints(sprintResp, query);
        } catch (Exception e) {
            log.error("Failed to fetch sprints for {}: {}", projectKey, e.getMessage());
            return getMockSprints(projectKey);
        }
    }

    // ── Components ────────────────────────────────────────────────────────────

    /**
     * Returns all components for a JIRA project, filtered by query.
     * JIRA API: GET /rest/api/3/project/{projectKey}/components
     */
    public List<JiraComponent> searchComponents(String projectKey, String query) {
        if (useMock) return filterMock(getMockComponents(), query,
                c -> c.getName().toLowerCase().contains(query.toLowerCase()));
        try {
            String url = jiraBaseUrl + "/rest/api/3/project/" + projectKey + "/components";
            String resp = buildClient().get().uri(url).retrieve().bodyToMono(String.class).block();
            return parseComponents(resp, query);
        } catch (Exception e) {
            log.error("Failed to fetch components for {}: {}", projectKey, e.getMessage());
            return getMockComponents();
        }
    }

    // ── Fix Versions ──────────────────────────────────────────────────────────

    /**
     * Returns unreleased fix versions for a JIRA project, filtered by query.
     * JIRA API: GET /rest/api/3/project/{projectKey}/versions
     */
    public List<JiraVersion> searchVersions(String projectKey, String query) {
        if (useMock) return filterMock(getMockVersions(), query,
                v -> v.getName().toLowerCase().contains(query.toLowerCase()));
        try {
            String url = jiraBaseUrl + "/rest/api/3/project/" + projectKey + "/versions";
            String resp = buildClient().get().uri(url).retrieve().bodyToMono(String.class).block();
            return parseVersions(resp, query);
        } catch (Exception e) {
            log.error("Failed to fetch versions for {}: {}", projectKey, e.getMessage());
            return getMockVersions();
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    /**
     * Searches JIRA users by display name or email for the Reporter field.
     * JIRA API: GET /rest/api/3/user/search?query={q}
     */
    public List<JiraUser> searchUsers(String query) {
        if (useMock) return filterMock(getMockUsers(), query,
                u -> u.getDisplayName().toLowerCase().contains(query.toLowerCase())
                  || u.getEmailAddress().toLowerCase().contains(query.toLowerCase()));
        try {
            String url = jiraBaseUrl + "/rest/api/3/user/search?query="
                    + java.net.URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&maxResults=10";
            String resp = buildClient().get().uri(url).retrieve().bodyToMono(String.class).block();
            return parseUsers(resp);
        } catch (Exception e) {
            log.error("Failed to search users for query '{}': {}", query, e.getMessage());
            return getMockUsers();
        }
    }

    // ── Epics ─────────────────────────────────────────────────────────────────

    /**
     * Resolves a single epic key to its name and status.
     * JIRA API: GET /rest/api/3/issue/{epicKey}?fields=summary,status
     */
    public Optional<JiraEpic> getEpic(String epicKey) {
        if (epicKey == null || epicKey.isBlank()) return Optional.empty();
        if (useMock) return getMockEpics().stream()
                .filter(e -> e.getIssueKey().equalsIgnoreCase(epicKey.trim()))
                .findFirst();
        try {
            String url = jiraBaseUrl + "/rest/api/3/issue/" + epicKey.trim()
                    + "?fields=summary,status,issuetype";
            String resp = buildClient().get().uri(url).retrieve().bodyToMono(String.class).block();
            JsonNode root = objectMapper.readTree(resp);
            String type = root.at("/fields/issuetype/name").asText("Epic");
            if (!type.equalsIgnoreCase("Epic")) {
                return Optional.empty(); // not an epic
            }
            return Optional.of(JiraEpic.builder()
                    .issueKey(root.path("key").asText())
                    .name(root.at("/fields/summary").asText())
                    .status(root.at("/fields/status/name").asText())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to resolve epic '{}': {}", epicKey, e.getMessage());
            return Optional.empty();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private WebClient buildClient() {
        String credentials = jiraUserEmail + ":" + jiraApiToken;
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return webClientBuilder.build().mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    private <T> List<T> filterMock(List<T> items, String query,
                                   java.util.function.Predicate<T> predicate) {
        if (query == null || query.isBlank()) return items;
        return items.stream().filter(predicate).collect(Collectors.toList());
    }

    private List<JiraSprint> parseSprints(String json, String query) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<JiraSprint> results = new ArrayList<>();
        for (JsonNode n : root.path("values")) {
            String name = n.path("name").asText();
            if (query == null || query.isBlank() || name.toLowerCase().contains(query.toLowerCase())) {
                results.add(JiraSprint.builder()
                        .id(n.path("id").asLong())
                        .name(name)
                        .state(n.path("state").asText())
                        .goal(n.path("goal").asText(""))
                        .build());
            }
        }
        return results;
    }

    private List<JiraComponent> parseComponents(String json, String query) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<JiraComponent> results = new ArrayList<>();
        for (JsonNode n : root) {
            String name = n.path("name").asText();
            if (query == null || query.isBlank() || name.toLowerCase().contains(query.toLowerCase())) {
                results.add(JiraComponent.builder()
                        .id(n.path("id").asText())
                        .name(name)
                        .description(n.path("description").asText(""))
                        .build());
            }
        }
        return results;
    }

    private List<JiraVersion> parseVersions(String json, String query) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<JiraVersion> results = new ArrayList<>();
        for (JsonNode n : root) {
            if (n.path("archived").asBoolean(false)) continue;
            String name = n.path("name").asText();
            if (query == null || query.isBlank() || name.toLowerCase().contains(query.toLowerCase())) {
                results.add(JiraVersion.builder()
                        .id(n.path("id").asText())
                        .name(name)
                        .description(n.path("description").asText(""))
                        .released(n.path("released").asBoolean(false))
                        .build());
            }
        }
        return results;
    }

    private List<JiraUser> parseUsers(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<JiraUser> results = new ArrayList<>();
        for (JsonNode n : root) {
            results.add(JiraUser.builder()
                    .accountId(n.path("accountId").asText())
                    .displayName(n.path("displayName").asText())
                    .emailAddress(n.path("emailAddress").asText(""))
                    .avatarUrl(n.at("/avatarUrls/24x24").asText(""))
                    .build());
        }
        return results;
    }

    // ── Mock data ─────────────────────────────────────────────────────────────

    private List<JiraSprint> getMockSprints(String projectKey) {
        String prefix = projectKey != null ? projectKey : "COMMSSURV";
        return List.of(
            JiraSprint.builder().id(41L).name(prefix + " Sprint 41 — Q1 Wrap-up")
                .state("closed").build(),
            JiraSprint.builder().id(42L).name(prefix + " Sprint 42 — Platform Hardening")
                .state("active").goal("Stabilise payments module and fix aging stories")
                .startDate(LocalDate.now().minusDays(7))
                .endDate(LocalDate.now().plusDays(7)).build(),
            JiraSprint.builder().id(43L).name(prefix + " Sprint 43 — Auth & Observability")
                .state("future").build(),
            JiraSprint.builder().id(44L).name(prefix + " Sprint 44 — Release Prep")
                .state("future").build()
        );
    }

    private List<JiraComponent> getMockComponents() {
        return List.of(
            JiraComponent.builder().id("10001").name("Payments")
                .description("Payment processing and gateway integration").build(),
            JiraComponent.builder().id("10002").name("Authentication")
                .description("SSO, JWT, and user identity").build(),
            JiraComponent.builder().id("10003").name("Notifications")
                .description("Email, SMS, and push notifications").build(),
            JiraComponent.builder().id("10004").name("Reporting")
                .description("Analytics and reporting dashboards").build(),
            JiraComponent.builder().id("10005").name("API Gateway")
                .description("Routing, rate limiting, and JWT validation").build(),
            JiraComponent.builder().id("10006").name("Search")
                .description("Elasticsearch-backed product and user search").build(),
            JiraComponent.builder().id("10007").name("Onboarding")
                .description("Tenant onboarding and setup workflows").build()
        );
    }

    private List<JiraVersion> getMockVersions() {
        return List.of(
            JiraVersion.builder().id("v001").name("v2.3.0")
                .description("Q1 release — auth improvements").released(false)
                .releaseDate(LocalDate.now().plusDays(14)).build(),
            JiraVersion.builder().id("v002").name("v2.4.0")
                .description("Q2 release — payments module").released(false)
                .releaseDate(LocalDate.now().plusDays(42)).build(),
            JiraVersion.builder().id("v003").name("v2.5.0")
                .description("Q3 release — observability stack").released(false)
                .releaseDate(LocalDate.now().plusDays(84)).build(),
            JiraVersion.builder().id("v004").name("v2.2.1-hotfix")
                .description("Production hotfix for payment timeout").released(false)
                .releaseDate(LocalDate.now().plusDays(3)).build()
        );
    }

    private List<JiraUser> getMockUsers() {
        return List.of(
            JiraUser.builder().accountId("acc-001").displayName("Esha Basu")
                .emailAddress("esha@db.com").build(),
            JiraUser.builder().accountId("acc-002").displayName("Sachin Tester")
                .emailAddress("sachin@db.com").build(),
            JiraUser.builder().accountId("acc-003").displayName("Lokesh Dev")
                .emailAddress("lokesh@db.com").build(),
            JiraUser.builder().accountId("acc-004").displayName("Abhay Product")
                .emailAddress("abhay@db.com").build(),
            JiraUser.builder().accountId("acc-005").displayName("Sharique Lead")
                .emailAddress("sharique@db.com").build()
        );
    }

    private List<JiraEpic> getMockEpics() {
        return List.of(
            JiraEpic.builder().issueKey("COMMSSURV-5").name("Authentication Platform Overhaul")
                .status("In Progress").color("purple").build(),
            JiraEpic.builder().issueKey("COMMSSURV-8").name("Payments Gateway v2")
                .status("In Progress").color("blue").build(),
            JiraEpic.builder().issueKey("COMMSSURV-12").name("Observability & Alerting Suite")
                .status("To Do").color("green").build(),
            JiraEpic.builder().issueKey("COMMSSURV-15").name("Developer Onboarding Experience")
                .status("Done").color("yellow").build(),
            JiraEpic.builder().issueKey("COMMSSURV-20").name("Multi-Tenant Admin Portal")
                .status("In Progress").color("orange").build()
        );
    }
}
