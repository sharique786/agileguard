-- =============================================================================
-- AgileGuard — Google BigQuery DDL
-- Dataset prefix: agileguard
-- =============================================================================
-- Execution order:
--   1. Create datasets (run once per project)
--   2. Create tables in dependency order:
--      core.ag_tenants → core.ag_project_teams → core.ag_feature_teams
--      → core.ag_users
--      → reports.ag_gap_findings, reports.ag_story_quality_log,
--        reports.ag_sprint_health, reports.ag_github_runs,
--        reports.ag_leaderboard_snapshots
--      → audit.ag_audit_log
--
-- Partitioning strategy:
--   All tables partition on DATE(created_at) or DATE(detected_at)
--   so daily/monthly query cost is predictable.
--
-- Clustering:
--   tenant_id is always the first cluster column for multi-tenant isolation.
--   project_id is second where applicable.
--
-- Naming conventions:
--   All table names: ag_{entity}
--   All column names: snake_case
--   Primary keys:    {entity}_id  (STRING, UUID)
--   Timestamps:      created_at, updated_at, detected_at  (TIMESTAMP)
--   Booleans:        is_{adjective} (is_active, is_resolved, is_flagged)
-- =============================================================================


-- =============================================================================
-- STEP 1 — Create datasets
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS `agileguard.core`
  OPTIONS (
    description = 'AgileGuard core domain: tenants, projects, teams, users',
    location     = 'US'
  );

CREATE SCHEMA IF NOT EXISTS `agileguard.reports`
  OPTIONS (
    description = 'AgileGuard reporting: gap findings, quality logs, sprint health, CI/CD, leaderboard',
    location     = 'US'
  );

CREATE SCHEMA IF NOT EXISTS `agileguard.audit`
  OPTIONS (
    description = 'AgileGuard immutable audit trail',
    location     = 'US'
  );


-- =============================================================================
-- STEP 2A — core.ag_tenants
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.core.ag_tenants` (

  -- Identity
  tenant_id       STRING  NOT NULL,   -- UUID, primary key
  name            STRING  NOT NULL,   -- "DB Corporation"
  slug            STRING  NOT NULL,   -- "db"  (unique, URL-safe)

  -- Subscription
  plan            STRING  NOT NULL,   -- FREE | PRO | ENTERPRISE
  is_active       BOOL    NOT NULL DEFAULT TRUE,

  -- JIRA connection (tokens stored in GCP Secret Manager; only reference stored here)
  jira_base_url   STRING,             -- "https://db.atlassian.net"
  jira_user_email STRING,             -- "svc-agileguard@db.com"
  jira_secret_ref STRING,             -- Secret Manager resource name

  -- Metadata
  created_at      TIMESTAMP NOT NULL,
  updated_at      TIMESTAMP

)
PARTITION BY DATE(created_at)
CLUSTER BY tenant_id
OPTIONS (
  description = 'Top-level tenant (organisation) registry. One row per customer.'
);


-- =============================================================================
-- STEP 2B — core.ag_project_teams
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.core.ag_project_teams` (

  project_team_id  STRING  NOT NULL,  -- UUID
  tenant_id        STRING  NOT NULL,  -- FK → ag_tenants.tenant_id
  name             STRING  NOT NULL,  -- "Platform Team"
  jira_project_key STRING,            -- "COMMSSURV"
  github_org       STRING,            -- "db-platform"
  is_active        BOOL    NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMP NOT NULL,
  updated_at       TIMESTAMP

)
PARTITION BY DATE(created_at)
CLUSTER BY tenant_id, project_team_id
OPTIONS (
  description = 'Project team within a tenant. Maps to exactly one JIRA project key.'
);


-- =============================================================================
-- STEP 2C — core.ag_feature_teams
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.core.ag_feature_teams` (

  feature_team_id  STRING  NOT NULL,  -- UUID
  project_team_id  STRING  NOT NULL,  -- FK → ag_project_teams.project_team_id
  tenant_id        STRING  NOT NULL,  -- denormalised for efficient filtering
  name             STRING  NOT NULL,  -- "Payments Feature Team"
  jira_component   STRING,            -- "Payments"
  github_repos     STRING,            -- comma-separated: "payments-api,checkout"
  is_active        BOOL    NOT NULL DEFAULT TRUE,
  created_at       TIMESTAMP NOT NULL,
  updated_at       TIMESTAMP

)
PARTITION BY DATE(created_at)
CLUSTER BY tenant_id, project_team_id
OPTIONS (
  description = 'Feature team under a project team. Owns one or more JIRA components.'
);


-- =============================================================================
-- STEP 2D — core.ag_users
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.core.ag_users` (

  user_id          STRING  NOT NULL,  -- UUID
  tenant_id        STRING  NOT NULL,  -- FK → ag_tenants.tenant_id
  project_team_id  STRING,            -- optional FK (denormalised)
  feature_team_id  STRING,            -- FK → ag_feature_teams.feature_team_id

  -- Identity
  email            STRING  NOT NULL,
  full_name        STRING  NOT NULL,
  role             STRING  NOT NULL,  -- SUPER_ADMIN | TENANT_ADMIN | PROJECT_ADMIN |
                                       -- FEATURE_LEAD | PRODUCT_OWNER | DEVELOPER |
                                       -- QA_TESTER | VIEWER

  -- External system references
  jira_account_id  STRING,            -- Atlassian account ID
  github_username  STRING,            -- GitHub login

  -- State
  is_active        BOOL    NOT NULL DEFAULT TRUE,
  last_login_at    TIMESTAMP,
  created_at       TIMESTAMP NOT NULL,
  updated_at       TIMESTAMP

)
PARTITION BY DATE(created_at)
CLUSTER BY tenant_id, feature_team_id
OPTIONS (
  description = 'Application users. Passwords are never stored here — managed by Auth service.'
);


-- =============================================================================
-- STEP 3A — reports.ag_gap_findings
-- One row per individual gap finding per issue per scan run.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.reports.ag_gap_findings` (

  finding_id       STRING  NOT NULL,  -- UUID
  tenant_id        STRING  NOT NULL,
  project_team_id  STRING  NOT NULL,
  feature_team_id  STRING,
  scan_run_id      STRING,            -- groups findings from the same scheduled scan

  -- Issue reference
  jira_issue_key   STRING  NOT NULL,  -- "COMMSSURV-123"
  issue_summary    STRING,
  sprint_id        STRING,
  sprint_name      STRING,

  -- Finding detail
  gap_type         STRING  NOT NULL,  -- MISSING_DESCRIPTION | MISSING_ACCEPTANCE_CRITERIA |
                                       -- MISSING_STORY_POINTS | MISSING_DEV_SUBTASK |
                                       -- MISSING_QA_SUBTASK | NO_EFFORT_LOGGED |
                                       -- AGING_STORY | INVALID_AC_FORMAT |
                                       -- MISSING_PR_LINK | COMPONENT_VERSION_CONFLICT
  severity         STRING  NOT NULL,  -- CRITICAL | ERROR | WARNING | INFO
  message          STRING,            -- human-readable finding description
  suggested_action STRING,            -- suggested remediation

  -- Resolution
  is_resolved      BOOL    NOT NULL DEFAULT FALSE,
  resolved_at      TIMESTAMP,
  resolved_by      STRING,            -- user_id

  -- Timestamps
  detected_at      TIMESTAMP NOT NULL,
  created_at       TIMESTAMP NOT NULL

)
PARTITION BY DATE(detected_at)
CLUSTER BY tenant_id, project_team_id, severity
OPTIONS (
  description = 'Individual gap findings. One row per rule violation per issue per scan.'
);


-- =============================================================================
-- STEP 3B — reports.ag_story_quality_log
-- One row per issue per scan run — captures the full quality snapshot.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.reports.ag_story_quality_log` (

  log_id           STRING  NOT NULL,  -- UUID
  tenant_id        STRING  NOT NULL,
  project_team_id  STRING  NOT NULL,
  feature_team_id  STRING,
  scan_run_id      STRING  NOT NULL,

  -- Issue reference
  jira_issue_key   STRING  NOT NULL,
  issue_summary    STRING,
  issue_type       STRING,            -- Story | Bug | Task
  issue_status     STRING,            -- TO_DO | IN_PROGRESS | READY_FOR_QA | …
  sprint_id        STRING,
  sprint_name      STRING,
  assignee_email   STRING,
  reporter_email   STRING,

  -- Quality metrics
  quality_score    INT64   NOT NULL,  -- 0–100
  ai_score         INT64,             -- Gemini AI score (may differ from rule-based)
  is_flagged       BOOL    NOT NULL,  -- score < 40 OR any CRITICAL finding

  -- Field completeness
  has_description  BOOL    NOT NULL,
  has_ac           BOOL    NOT NULL,
  has_story_points BOOL    NOT NULL,
  has_dev_subtask  BOOL    NOT NULL,
  has_qa_subtask   BOOL    NOT NULL,
  effort_logged_min INT64,            -- total minutes logged
  days_in_status   INT64,             -- days in current status at scan time

  -- New enriched fields (Story Editor v2)
  priority         STRING,            -- Blocker | Critical | Major | Medium | …
  business_line    STRING,            -- Business | GT | Platform | …
  epic_link        STRING,            -- "COMMSSURV-5"
  components       STRING,            -- JSON array: ["Payments","Auth"]
  fix_versions     STRING,            -- JSON array: ["v2.4.0"]
  labels           STRING,            -- JSON array: ["tech-debt","security"]
  team_names       STRING,            -- JSON array: ["Payments Team"]

  -- Finding counts
  critical_count   INT64   NOT NULL DEFAULT 0,
  error_count      INT64   NOT NULL DEFAULT 0,
  warning_count    INT64   NOT NULL DEFAULT 0,
  total_findings   INT64   NOT NULL DEFAULT 0,

  evaluated_at     TIMESTAMP NOT NULL,
  created_at       TIMESTAMP NOT NULL

)
PARTITION BY DATE(evaluated_at)
CLUSTER BY tenant_id, project_team_id, is_flagged
OPTIONS (
  description = 'Complete quality snapshot per issue per scan run. Core reporting table.'
);


-- =============================================================================
-- STEP 3C — reports.ag_sprint_health
-- One row per sprint per scan run — aggregated health metrics.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.reports.ag_sprint_health` (

  sprint_health_id  STRING  NOT NULL,  -- UUID
  tenant_id         STRING  NOT NULL,
  project_team_id   STRING  NOT NULL,
  scan_run_id       STRING  NOT NULL,

  -- Sprint reference
  sprint_id         STRING,
  sprint_name       STRING,
  sprint_state      STRING,            -- active | closed | future
  sprint_goal       STRING,
  sprint_start_date DATE,
  sprint_end_date   DATE,

  -- Health metrics
  total_issues      INT64   NOT NULL,
  issues_with_gaps  INT64   NOT NULL,
  flagged_issues    INT64   NOT NULL,
  avg_quality_score FLOAT64 NOT NULL,  -- 0.0–100.0
  health_score      INT64   NOT NULL,  -- rounded 0–100

  -- Aging
  aging_issue_keys  STRING,            -- JSON array of issue keys past threshold

  -- Velocity
  planned_points    INT64,
  completed_points  INT64,
  velocity_ratio    FLOAT64,           -- completed / planned

  -- Team breakdown (JSON for flexibility)
  team_scores       STRING,            -- JSON: [{"teamId":"…","score":85}]

  scanned_at        TIMESTAMP NOT NULL,
  created_at        TIMESTAMP NOT NULL

)
PARTITION BY DATE(scanned_at)
CLUSTER BY tenant_id, project_team_id
OPTIONS (
  description = 'Sprint-level health aggregate. One row per sprint per scan run.'
);


-- =============================================================================
-- STEP 3D — reports.ag_github_runs
-- GitHub Actions workflow run history linked to JIRA issues.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.reports.ag_github_runs` (

  run_record_id     STRING  NOT NULL,  -- UUID (our internal ID)
  tenant_id         STRING  NOT NULL,
  project_team_id   STRING,

  -- GitHub identifiers
  github_run_id     INT64   NOT NULL,  -- GitHub's numeric run ID
  workflow_name     STRING,
  repo_full_name    STRING,            -- "db-platform/payments-api"
  head_branch       STRING,
  head_sha          STRING,

  -- Run result
  status            STRING,            -- queued | in_progress | completed
  conclusion        STRING,            -- success | failure | cancelled | skipped | null
  triggered_by      STRING,            -- GitHub username
  run_url           STRING,

  -- JIRA linkage (extracted from branch name convention: feature/COMMSSURV-123-description)
  jira_issue_key    STRING,

  -- Timing
  duration_seconds  INT64,
  run_started_at    TIMESTAMP,
  run_completed_at  TIMESTAMP,
  created_at        TIMESTAMP NOT NULL

)
PARTITION BY DATE(run_started_at)
CLUSTER BY tenant_id, project_team_id, conclusion
OPTIONS (
  description = 'GitHub Actions workflow runs. Linked to JIRA issues via branch naming convention.'
);


-- =============================================================================
-- STEP 3E — reports.ag_leaderboard_snapshots
-- Per-sprint leaderboard scores and badges per contributor.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.reports.ag_leaderboard_snapshots` (

  snapshot_id       STRING  NOT NULL,  -- UUID
  tenant_id         STRING  NOT NULL,
  project_team_id   STRING  NOT NULL,
  feature_team_id   STRING,

  -- Sprint reference
  sprint_id         STRING  NOT NULL,
  sprint_name       STRING,

  -- Contributor
  user_id           STRING  NOT NULL,  -- FK → ag_users.user_id
  full_name         STRING,
  github_username   STRING,
  role              STRING,            -- DEVELOPER | QA_TESTER | PRODUCT_OWNER | …

  -- Composite score (0–100)
  composite_score   INT64   NOT NULL,
  rank_in_sprint    INT64,             -- 1 = highest scorer

  -- Score components
  story_quality_score   FLOAT64,       -- avg AI score of owned stories (30%)
  effort_logging_score  FLOAT64,       -- % of in-progress days with effort logged (20%)
  subtask_compliance    FLOAT64,       -- % of stories with dev+QA subtasks (15%)
  pr_review_score       FLOAT64,       -- weighted PR reviews contributed (15%)
  ci_pass_rate          FLOAT64,       -- % of their pipelines passing (10%)
  commit_frequency      FLOAT64,       -- normalised commits per sprint (10%)

  -- Raw counts
  stories_delivered INT64,
  commit_count      INT64,
  pr_count          INT64,
  pr_review_count   INT64,
  ci_runs_total     INT64,
  ci_runs_passed    INT64,

  -- Badges (JSON array of badge names)
  badges            STRING,            -- ["QUALITY_CHAMPION","TOP_REVIEWER"]

  snapshot_at       TIMESTAMP NOT NULL,
  created_at        TIMESTAMP NOT NULL

)
PARTITION BY DATE(snapshot_at)
CLUSTER BY tenant_id, project_team_id, sprint_id
OPTIONS (
  description = 'Developer and QA leaderboard snapshot per sprint. Append-only.'
);


-- =============================================================================
-- STEP 4 — audit.ag_audit_log
-- Immutable append-only audit trail for all data mutations.
-- =============================================================================

CREATE TABLE IF NOT EXISTS `agileguard.audit.ag_audit_log` (

  audit_id         STRING  NOT NULL,  -- UUID
  tenant_id        STRING  NOT NULL,

  -- Who
  user_id          STRING,
  user_email       STRING,
  user_role        STRING,
  ip_address       STRING,
  user_agent       STRING,

  -- What
  action           STRING  NOT NULL,  -- LOGIN | LOGOUT | TENANT_CREATED | USER_INVITED |
                                       -- PROJECT_CREATED | FEATURE_TEAM_CREATED |
                                       -- STORY_CREATED | STORY_UPDATED |
                                       -- TRANSITION_ATTEMPTED | TRANSITION_BLOCKED |
                                       -- TRANSITION_ALLOWED | GAP_DETECTED |
                                       -- GAP_RESOLVED | SCAN_STARTED | SCAN_COMPLETED |
                                       -- USER_ROLE_CHANGED | USER_DEACTIVATED |
                                       -- TENANT_UPDATED | EXPORT_GENERATED

  -- Target entity
  entity_type      STRING,            -- TENANT | PROJECT_TEAM | FEATURE_TEAM | USER |
                                       -- JIRA_ISSUE | GAP_FINDING | SPRINT
  entity_id        STRING,
  entity_key       STRING,            -- human-readable (JIRA issue key, email, slug)

  -- Change detail (JSON, max 10 KB)
  old_value        STRING,            -- JSON snapshot before change
  new_value        STRING,            -- JSON snapshot after change
  metadata         STRING,            -- extra context as JSON

  -- Result
  is_success       BOOL    NOT NULL DEFAULT TRUE,
  failure_reason   STRING,

  created_at       TIMESTAMP NOT NULL

)
PARTITION BY DATE(created_at)
CLUSTER BY tenant_id, action
OPTIONS (
  description = 'Immutable audit log. Never update or delete rows. Append-only.'
);


-- =============================================================================
-- VIEWS — convenience queries for dashboards
-- =============================================================================

-- Active tenants summary
CREATE OR REPLACE VIEW `agileguard.core.v_active_tenants` AS
SELECT
  t.tenant_id,
  t.name                                          AS tenant_name,
  t.slug,
  t.plan,
  COUNT(DISTINCT pt.project_team_id)              AS project_team_count,
  COUNT(DISTINCT ft.feature_team_id)              AS feature_team_count,
  COUNT(DISTINCT u.user_id)                       AS user_count,
  t.jira_base_url,
  t.created_at
FROM `agileguard.core.ag_tenants`        t
LEFT JOIN `agileguard.core.ag_project_teams` pt ON pt.tenant_id = t.tenant_id AND pt.is_active
LEFT JOIN `agileguard.core.ag_feature_teams` ft ON ft.tenant_id = t.tenant_id AND ft.is_active
LEFT JOIN `agileguard.core.ag_users`         u  ON u.tenant_id  = t.tenant_id AND u.is_active
WHERE t.is_active
GROUP BY 1,2,3,4,5,8,9;


-- Latest sprint health per project (most recent scan per sprint)
CREATE OR REPLACE VIEW `agileguard.reports.v_latest_sprint_health` AS
SELECT
  sh.*
FROM `agileguard.reports.ag_sprint_health` sh
INNER JOIN (
  SELECT project_team_id, sprint_id, MAX(scanned_at) AS latest_scan
  FROM `agileguard.reports.ag_sprint_health`
  GROUP BY 1, 2
) latest
  ON  sh.project_team_id = latest.project_team_id
  AND sh.sprint_id        = latest.sprint_id
  AND sh.scanned_at       = latest.latest_scan;


-- Open (unresolved) gap findings with issue details
CREATE OR REPLACE VIEW `agileguard.reports.v_open_gap_findings` AS
SELECT
  gf.finding_id,
  gf.tenant_id,
  gf.project_team_id,
  gf.feature_team_id,
  gf.jira_issue_key,
  gf.issue_summary,
  gf.sprint_name,
  gf.gap_type,
  gf.severity,
  gf.message,
  gf.suggested_action,
  gf.detected_at,
  TIMESTAMP_DIFF(CURRENT_TIMESTAMP(), gf.detected_at, DAY) AS days_open
FROM `agileguard.reports.ag_gap_findings` gf
WHERE gf.is_resolved = FALSE
ORDER BY
  CASE gf.severity WHEN 'CRITICAL' THEN 1 WHEN 'ERROR' THEN 2 WHEN 'WARNING' THEN 3 ELSE 4 END,
  gf.detected_at DESC;


-- Developer leaderboard — latest sprint per project
CREATE OR REPLACE VIEW `agileguard.reports.v_current_leaderboard` AS
SELECT
  ls.*
FROM `agileguard.reports.ag_leaderboard_snapshots` ls
INNER JOIN (
  SELECT project_team_id, MAX(snapshot_at) AS latest_snapshot
  FROM `agileguard.reports.ag_leaderboard_snapshots`
  GROUP BY 1
) latest
  ON  ls.project_team_id = latest.project_team_id
  AND ls.snapshot_at     = latest.latest_snapshot
ORDER BY ls.project_team_id, ls.rank_in_sprint;


-- CI/CD pass rate by repo (last 30 days)
CREATE OR REPLACE VIEW `agileguard.reports.v_cicd_health_30d` AS
SELECT
  tenant_id,
  project_team_id,
  repo_full_name,
  workflow_name,
  COUNT(*)                                                        AS total_runs,
  COUNTIF(conclusion = 'success')                                AS passed_runs,
  COUNTIF(conclusion = 'failure')                                AS failed_runs,
  ROUND(
    SAFE_DIVIDE(COUNTIF(conclusion = 'success'), COUNT(*)) * 100,
    1
  )                                                              AS pass_rate_pct,
  AVG(duration_seconds)                                          AS avg_duration_seconds,
  MAX(run_started_at)                                            AS last_run_at
FROM `agileguard.reports.ag_github_runs`
WHERE run_started_at >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 30 DAY)
  AND status = 'completed'
GROUP BY 1, 2, 3, 4;


-- Audit trail per tenant (last 7 days)
CREATE OR REPLACE VIEW `agileguard.audit.v_recent_activity` AS
SELECT
  audit_id,
  tenant_id,
  user_email,
  user_role,
  action,
  entity_type,
  entity_key,
  is_success,
  failure_reason,
  created_at
FROM `agileguard.audit.ag_audit_log`
WHERE created_at >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 7 DAY)
ORDER BY created_at DESC;


-- =============================================================================
-- SAMPLE QUERIES
-- =============================================================================

-- Q1: Stories currently flagged in a tenant's active sprint
-- SELECT jira_issue_key, issue_summary, quality_score, critical_count, error_count
-- FROM `agileguard.reports.ag_story_quality_log`
-- WHERE tenant_id = 'TENANT-ID'
--   AND is_flagged = TRUE
--   AND DATE(evaluated_at) = CURRENT_DATE()
-- ORDER BY quality_score ASC;

-- Q2: Sprint velocity trend over last 6 months
-- SELECT sprint_name, planned_points, completed_points, velocity_ratio, health_score, scanned_at
-- FROM `agileguard.reports.v_latest_sprint_health`
-- WHERE tenant_id = 'TENANT-ID'
--   AND project_team_id = 'PROJECT-ID'
-- ORDER BY scanned_at DESC
-- LIMIT 12;

-- Q3: Top gap types by frequency (helps prioritise coaching)
-- SELECT gap_type, severity, COUNT(*) AS occurrence_count
-- FROM `agileguard.reports.ag_gap_findings`
-- WHERE tenant_id = 'TENANT-ID'
--   AND detected_at >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL 90 DAY)
-- GROUP BY 1, 2
-- ORDER BY 3 DESC;

-- Q4: Leaderboard for a specific sprint
-- SELECT full_name, role, composite_score, rank_in_sprint, badges
-- FROM `agileguard.reports.ag_leaderboard_snapshots`
-- WHERE tenant_id = 'TENANT-ID'
--   AND sprint_id = 'SPRINT-ID'
-- ORDER BY rank_in_sprint;
