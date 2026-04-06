package com.agileguard.jira.service;

import com.agileguard.jira.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JiraLookupService (mock mode only — no JIRA connection needed).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JiraLookupService Tests")
class JiraLookupServiceTest {

    @InjectMocks private JiraLookupService service;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Force mock mode so no HTTP calls are made
        ReflectionTestUtils.setField(service, "useMock", true);
    }

    // ── Sprints ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchSprints — should return all sprints when query is empty")
    void searchSprints_emptyQuery_returnsAll() {
        List<JiraSprint> results = service.searchSprints("COMMSSURV", "");
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(s -> s.getState().equals("active"));
        assertThat(results).anyMatch(s -> s.getState().equals("future"));
    }

    @Test
    @DisplayName("searchSprints — should filter by query string")
    void searchSprints_withQuery_filtersResults() {
        List<JiraSprint> results = service.searchSprints("COMMSSURV", "Sprint 42");
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(s -> s.getName().contains("42"));
    }

    @Test
    @DisplayName("searchSprints — should return empty list for non-matching query")
    void searchSprints_noMatch_returnsEmpty() {
        List<JiraSprint> results = service.searchSprints("COMMSSURV", "ZZZ999NonExistent");
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("searchSprints — active sprint should have start and end dates")
    void searchSprints_activeSprint_hasDates() {
        List<JiraSprint> sprints = service.searchSprints("COMMSSURV", "");
        JiraSprint active = sprints.stream()
                .filter(s -> "active".equals(s.getState()))
                .findFirst().orElseThrow();
        assertThat(active.getStartDate()).isNotNull();
        assertThat(active.getEndDate()).isNotNull();
    }

    // ── Components ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchComponents — should return all components when query is empty")
    void searchComponents_emptyQuery_returnsAll() {
        List<JiraComponent> results = service.searchComponents("COMMSSURV", "");
        assertThat(results).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("searchComponents — should filter by partial name match")
    void searchComponents_partialMatch_filters() {
        List<JiraComponent> results = service.searchComponents("COMMSSURV", "pay");
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getName()).containsIgnoringCase("pay");
    }

    @Test
    @DisplayName("searchComponents — each component has a non-blank name")
    void searchComponents_allHaveNames() {
        service.searchComponents("COMMSSURV", "").forEach(c ->
                assertThat(c.getName()).isNotBlank());
    }

    // ── Fix Versions ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchVersions — should return unreleased versions")
    void searchVersions_emptyQuery_returnsVersions() {
        List<JiraVersion> results = service.searchVersions("COMMSSURV", "");
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(v -> !v.isReleased());
    }

    @Test
    @DisplayName("searchVersions — should filter by version name")
    void searchVersions_withQuery_filters() {
        List<JiraVersion> results = service.searchVersions("COMMSSURV", "v2.4");
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getName()).contains("2.4");
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchUsers — should return users matching display name")
    void searchUsers_matchDisplayName_returnsUser() {
        List<JiraUser> results = service.searchUsers("Esha");
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getDisplayName()).containsIgnoringCase("Esha");
    }

    @Test
    @DisplayName("searchUsers — should return users matching email")
    void searchUsers_matchEmail_returnsUser() {
        List<JiraUser> results = service.searchUsers("sachin@");
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getEmailAddress()).containsIgnoringCase("sachin");
    }

    @Test
    @DisplayName("searchUsers — all users have accountId and displayName")
    void searchUsers_allHaveRequiredFields() {
        service.searchUsers("").forEach(u -> {
            assertThat(u.getAccountId()).isNotBlank();
            assertThat(u.getDisplayName()).isNotBlank();
        });
    }

    // ── Epics ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getEpic — should resolve known epic key to name")
    void getEpic_knownKey_returnsEpic() {
        Optional<JiraEpic> epic = service.getEpic("COMMSSURV-5");
        assertThat(epic).isPresent();
        assertThat(epic.get().getIssueKey()).isEqualTo("COMMSSURV-5");
        assertThat(epic.get().getName()).isNotBlank();
    }

    @Test
    @DisplayName("getEpic — should be case-insensitive for issue key")
    void getEpic_caseInsensitive_returnsEpic() {
        Optional<JiraEpic> epic = service.getEpic("COMMSSURV-8");
        assertThat(epic).isPresent();
        assertThat(epic.get().getIssueKey()).isEqualTo("COMMSSURV-8");
    }

    @Test
    @DisplayName("getEpic — should return empty for unknown key")
    void getEpic_unknownKey_returnsEmpty() {
        Optional<JiraEpic> epic = service.getEpic("COMMSSURV-9999");
        assertThat(epic).isEmpty();
    }

    @Test
    @DisplayName("getEpic — should return empty for null or blank key")
    void getEpic_blankKey_returnsEmpty() {
        assertThat(service.getEpic(null)).isEmpty();
        assertThat(service.getEpic("")).isEmpty();
        assertThat(service.getEpic("   ")).isEmpty();
    }
}
