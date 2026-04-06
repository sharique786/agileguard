package com.agileguard.jira.service;

import com.agileguard.common.enums.IssueStatus;
import com.agileguard.common.enums.IssueType;
import com.agileguard.jira.model.JiraIssue;
import com.agileguard.jira.model.JiraSubTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * HTTP client for the Atlassian JIRA REST API v3.
 * Uses Spring WebClient for non-blocking calls.
 *
 * For local POC: configure jira.base-url, jira.user-email, jira.api-token
 * in application.yml to point to your Atlassian Cloud instance.
 * If not configured, the service returns mock data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JiraClientService {

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

    /**
     * Fetches all issues in the active sprint for a project.
     *
     * @param projectKey JIRA project key (e.g., "COMMSSURV")
     * @return list of JiraIssue objects
     */
    public List<JiraIssue> getActiveSprintIssues(String projectKey) {
        if (useMock) {
            log.info("Using mock JIRA data for project: {}", projectKey);
            return getMockIssues(projectKey);
        }

        try {
            String url = jiraBaseUrl + "/rest/api/3/search"
                    + "?jql=project=" + projectKey + "%20AND%20sprint%20in%20openSprints()"
                    + "&fields=summary,description,status,priority,assignee,issuetype,"
                    + "story_points,customfield_10016,subtasks,customfield_10014"
                    + "&maxResults=100";

            String response = buildClient().get().uri(url)
                    .retrieve().bodyToMono(String.class).block();

            return parseIssues(response);
        } catch (WebClientResponseException ex) {
            log.error("JIRA API error for project {}: {} - {}", projectKey,
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return List.of();
        }
    }

    /**
     * Fetches a single JIRA issue by its key.
     *
     * @param issueKey the JIRA issue key (e.g., "COMMSSURV-123")
     * @return optional JiraIssue
     */
    public Optional<JiraIssue> getIssue(String issueKey) {
        if (useMock) {
            return getMockIssues("COMMSSURV").stream()
                    .filter(i -> i.getKey().equals(issueKey)).findFirst();
        }

        try {
            String url = jiraBaseUrl + "/rest/api/3/issue/" + issueKey
                    + "?fields=summary,description,status,assignee,issuetype,"
                    + "story_points,customfield_10016,subtasks,customfield_10014";

            String response = buildClient().get().uri(url)
                    .retrieve().bodyToMono(String.class).block();

            JsonNode root = objectMapper.readTree(response);
            return Optional.of(parseIssue(root));
        } catch (Exception ex) {
            log.error("Failed to fetch JIRA issue {}: {}", issueKey, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Builds a WebClient with JIRA Basic Auth credentials. */
    private WebClient buildClient() {
        String credentials = jiraUserEmail + ":" + jiraApiToken;
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return webClientBuilder.build().mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    /** Parses JIRA search results JSON into JiraIssue list. */
    private List<JiraIssue> parseIssues(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<JiraIssue> issues = new ArrayList<>();
            for (JsonNode issue : root.get("issues")) {
                issues.add(parseIssue(issue));
            }
            return issues;
        } catch (Exception e) {
            log.error("Failed to parse JIRA response: {}", e.getMessage());
            return List.of();
        }
    }

    /** Parses a single JIRA issue node. */
    private JiraIssue parseIssue(JsonNode node) {
        JsonNode fields = node.get("fields");
        return JiraIssue.builder()
                .id(node.path("id").asText())
                .key(node.path("key").asText())
                .summary(fields.path("summary").asText())
                .description(fields.path("description").asText(""))
                .status(parseStatus(fields.path("status").path("name").asText()))
                .storyPoints(fields.path("customfield_10016").isNull()
                        ? null : fields.path("customfield_10016").asInt())
                .build();
    }

    private IssueStatus parseStatus(String statusName) {
        return switch (statusName.toLowerCase()) {
            case "in progress" -> IssueStatus.IN_PROGRESS;
            case "in review" -> IssueStatus.IN_REVIEW;
            case "ready for qa" -> IssueStatus.READY_FOR_QA;
            case "qa in progress" -> IssueStatus.QA_IN_PROGRESS;
            case "ready for release" -> IssueStatus.READY_FOR_RELEASE;
            case "done" -> IssueStatus.DONE;
            case "blocked" -> IssueStatus.BLOCKED;
            default -> IssueStatus.TO_DO;
        };
    }

    /**
     * Returns realistic mock JIRA issues for local development and testing.
     * Includes a mix of well-groomed and poorly-groomed stories to demonstrate the gap detector.
     */
    private List<JiraIssue> getMockIssues(String projectKey) {
        String prefix = projectKey != null ? projectKey : "COMMSSURV";
        return List.of(
            // Good story — fully groomed
            JiraIssue.builder()
                .id("10001").key(prefix + "-101").summary("User login with SSO")
                .description("As a user, I want to log in using Google SSO so that I don't need a separate password.")
                .acceptanceCriteria("Given a user with a Google account, When they click 'Login with Google', Then they are authenticated and redirected to the dashboard.")
                .issueType(IssueType.STORY).status(IssueStatus.IN_PROGRESS)
                .storyPoints(5).timeSpentMinutes(120)
                .statusChangedAt(LocalDate.now().minusDays(2))
                .subTasks(List.of(
                    JiraSubTask.builder().key(prefix + "-101-1").type("Dev Task").status("In Progress").build()
                ))
                .linkedPullRequests(List.of("PR-42"))
                .build(),

            // Bad story — missing AC and description
            JiraIssue.builder()
                .id("10002").key(prefix + "-102").summary("Fix payment bug")
                .description("") // Missing description
                .acceptanceCriteria(null) // Missing AC
                .issueType(IssueType.BUG).status(IssueStatus.IN_PROGRESS)
                .storyPoints(null) // No points
                .timeSpentMinutes(0) // No effort
                .statusChangedAt(LocalDate.now().minusDays(7)) // Aging!
                .build(),

            // Partial story — has description but bad AC format
            JiraIssue.builder()
                .id("10003").key(prefix + "-103").summary("Add product search API")
                .description("We need to build a search endpoint for the product catalogue.")
                .acceptanceCriteria("Search should work and return results") // Bad format
                .issueType(IssueType.STORY).status(IssueStatus.READY_FOR_QA)
                .storyPoints(8)
                .statusChangedAt(LocalDate.now().minusDays(1))
                .build(), // Missing QA sub-task

            // Good story — all gates passed
            JiraIssue.builder()
                .id("10004").key(prefix + "-104").summary("Implement order notifications")
                .description("As a buyer, I want to receive email notifications when my order status changes so that I am always informed.")
                .acceptanceCriteria("Given an order status change, When the event is received, Then an email is sent to the buyer within 2 minutes.")
                .issueType(IssueType.STORY).status(IssueStatus.QA_IN_PROGRESS)
                .storyPoints(3).timeSpentMinutes(240)
                .statusChangedAt(LocalDate.now().minusDays(1))
                .subTasks(List.of(
                    JiraSubTask.builder().key(prefix + "-104-1").type("Dev Task").status("Done").build(),
                    JiraSubTask.builder().key(prefix + "-104-2").type("QA Task").status("In Progress").build()
                ))
                .linkedPullRequests(List.of("PR-55"))
                .build()
        );
    }
}
