package com.agileguard.github.service;

import com.agileguard.github.model.WorkflowRun;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for the GitHub REST API v3.
 * When use-mock=true (default), returns demo data without real API calls.
 * Set github.token and github.use-mock=false for a real repository.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubClientService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${agileguard.github.token:#{null}}")
    private String githubToken;

    @Value("${agileguard.github.use-mock:true}")
    private boolean useMock;

    private static final String GITHUB_API = "https://api.github.com";

    /**
     * Returns recent workflow runs for a repository.
     *
     * @param owner     GitHub org or user
     * @param repo      repository name
     * @param maxResults max runs to return
     */
    public List<WorkflowRun> getWorkflowRuns(String owner, String repo, int maxResults) {
        if (useMock) return getMockWorkflowRuns(owner, repo);

        try {
            String url = GITHUB_API + "/repos/" + owner + "/" + repo
                    + "/actions/runs?per_page=" + maxResults;

            String response = buildClient().get().uri(url)
                    .retrieve().bodyToMono(String.class).block();

            return parseWorkflowRuns(response, owner + "/" + repo);
        } catch (Exception e) {
            log.error("GitHub API error for {}/{}: {}", owner, repo, e.getMessage());
            return getMockWorkflowRuns(owner, repo);
        }
    }

    /** Builds an authenticated WebClient for GitHub API calls. */
    private WebClient buildClient() {
        WebClient.Builder builder = webClientBuilder.clone()
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");
        if (githubToken != null) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken);
        }
        return builder.build();
    }

    private List<WorkflowRun> parseWorkflowRuns(String json, String repoFullName) {
        List<WorkflowRun> runs = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            for (JsonNode run : root.get("workflow_runs")) {
                runs.add(WorkflowRun.builder()
                        .id(run.path("id").asLong())
                        .name(run.path("name").asText())
                        .headBranch(run.path("head_branch").asText())
                        .status(run.path("status").asText())
                        .conclusion(run.path("conclusion").asText())
                        .workflowName(run.path("name").asText())
                        .repoFullName(repoFullName)
                        .startedAt(Instant.parse(run.path("run_started_at").asText()))
                        .build());
            }
        } catch (Exception e) {
            log.error("Error parsing GitHub workflow runs: {}", e.getMessage());
        }
        return runs;
    }

    /** Returns realistic mock GitHub Actions run data. */
    private List<WorkflowRun> getMockWorkflowRuns(String owner, String repo) {
        String fullName = owner + "/" + repo;
        return List.of(
            WorkflowRun.builder()
                .id(1001L).name("CI Pipeline").headBranch("main")
                .status("completed").conclusion("success")
                .workflowName("CI Pipeline").repoFullName(fullName)
                .triggeredBy("esha-dev").jiraIssueKey("COMMSSURV-101")
                .startedAt(Instant.now().minusSeconds(3600))
                .completedAt(Instant.now().minusSeconds(3300))
                .durationSeconds(300).htmlUrl("https://github.com/" + fullName + "/actions/runs/1001")
                .build(),
            WorkflowRun.builder()
                .id(1002L).name("CI Pipeline").headBranch("feature/COMMSSURV-102-payment-fix")
                .status("completed").conclusion("failure")
                .workflowName("CI Pipeline").repoFullName(fullName)
                .triggeredBy("sachin-qa").jiraIssueKey("COMMSSURV-102")
                .startedAt(Instant.now().minusSeconds(7200))
                .completedAt(Instant.now().minusSeconds(6900))
                .durationSeconds(300).htmlUrl("https://github.com/" + fullName + "/actions/runs/1002")
                .build(),
            WorkflowRun.builder()
                .id(1003L).name("Security Scan").headBranch("main")
                .status("completed").conclusion("success")
                .workflowName("Security Scan").repoFullName(fullName)
                .triggeredBy("db-admin")
                .startedAt(Instant.now().minusSeconds(1800))
                .completedAt(Instant.now().minusSeconds(1500))
                .durationSeconds(300).build()
        );
    }
}
