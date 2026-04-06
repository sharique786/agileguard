# 🛡️ AgileGuard — SDLC Governance Platform

> Multi-tenant JIRA governance wrapper with AI-powered story validation, configurable gap detection, real-time CI/CD monitoring, tenant onboarding, and a full admin panel.

---

## Table of Contents

1. [What Is AgileGuard](#1-what-is-agileguard)
2. [Architecture Overview](#2-architecture-overview)
3. [Technology Stack](#3-technology-stack)
4. [Prerequisites](#4-prerequisites)
5. [Quick Start](#5-quick-start)
6. [Module Structure](#6-module-structure)
7. [Frontend Pages](#7-frontend-pages)
8. [Complete API Reference](#8-complete-api-reference)
9. [Gap Detection Engine](#9-gap-detection-engine)
10. [Transition Guard Gates](#10-transition-guard-gates)
11. [Story Editor — Enhanced Fields](#11-story-editor--enhanced-fields)
12. [Tenant Onboarding Workflow](#12-tenant-onboarding-workflow)
13. [Admin Panel](#13-admin-panel)
14. [Gemini AI Integration](#14-gemini-ai-integration)
15. [GitHub CI/CD Integration](#15-github-cicd-integration)
16. [Developer Leaderboard](#16-developer-leaderboard)
17. [Gap Reports and Export](#17-gap-reports-and-export)
18. [Multi-Environment Configuration](#18-multi-environment-configuration)
19. [RBAC — Roles and Permissions](#19-rbac--roles-and-permissions)
20. [Connecting to Real APIs](#20-connecting-to-real-apis)
21. [Running Tests](#21-running-tests)
22. [Docker Deployment](#22-docker-deployment)
23. [BigQuery Schema — Production](#23-bigquery-schema--production)

---

## 1. What Is AgileGuard

AgileGuard is a **multi-tenant SDLC governance platform** built on top of Atlassian JIRA. It enforces quality standards at story creation time, detects process gaps proactively via scheduled scans, and provides AI-powered guidance to Product Owners and developers throughout the sprint lifecycle.

### Problems It Solves

| # | Problem | AgileGuard Solution |
|---|---------|-------------------|
| 1 | Stories enter sprints without Description or Acceptance Criteria | Mandatory field enforcement + real-time AI quality scoring |
| 2 | AC is present but vague or unstructured | Gemini AI suggestions with Given/When/Then generation and apply-in-one-click |
| 3 | Developers and QA do not create sub-tasks or log effort | Automated sub-task enforcement and effort-logging compliance rules |
| 4 | Stories move to Ready for Release without passing quality gates | Transition Guard blocks invalid status changes in real time |
| 5 | Component version conflicts across feature teams | Cross-team component registry with conflict detection |
| 6 | No visibility into which teams deliver quality | Sprint health reports, gap scores per story, and developer leaderboard |

---

## 2. Architecture Overview

```
┌──────────────────────────────────────────────────────────────────┐
│             Angular 17 SPA  (port 4200)                          │
│  Login · Onboarding Wizard · Dashboard · Story Editor            │
│  Gap Reports · Leaderboard · Admin Panel                         │
└───────────────────────────┬──────────────────────────────────────┘
                            │ HTTPS / WebSocket (SSE)
┌───────────────────────────▼──────────────────────────────────────┐
│         Spring Cloud Gateway  (port 8080)                        │
│  JWT validation · X-Tenant-ID injection · CORS · Rate limiting   │
└──┬───────────────┬──────────────────┬────────────────┬───────────┘
   │               │                  │                │
┌──▼──────┐  ┌─────▼──────┐  ┌────────▼────┐  ┌──────▼──────┐
│  Auth   │  │   JIRA     │  │  Gemini AI  │  │   GitHub    │
│ Service │  │  Service   │  │  Service    │  │   Service   │
│  :8081  │  │   :8082    │  │   :8083     │  │    :8084    │
└──┬──────┘  └─────┬──────┘  └────────┬────┘  └──────┬──────┘
   │               │                  │               │
   │          ┌────▼──────┐  ┌────────▼────┐  ┌──────▼──────┐
   │          │ Atlassian │  │  Google     │  │  GitHub     │
   │          │  JIRA v3  │  │  Gemini API │  │  REST API   │
   │          └───────────┘  └─────────────┘  └─────────────┘
   │
┌──▼──────────────────────────────────────────────────────────┐
│  Local:  H2 in-memory DB (zero config, auto-seeded)          │
│  Prod:   Google BigQuery + Cloud SQL (PostgreSQL)            │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend framework | Spring Boot | 3.2.x |
| API Gateway | Spring Cloud Gateway | 2023.0.1 |
| Language | Java | 21 (LTS) |
| Build | Maven | 3.9+ |
| Frontend | Angular (standalone components) | 17+ |
| Frontend language | TypeScript | 5.4 |
| Authentication | Spring Security + JJWT | 6.x / 0.12.x |
| ORM (local) | Spring Data JPA + H2 | 3.2.x |
| Database (local) | H2 In-Memory | — |
| Database (prod) | Google BigQuery / Cloud SQL | — |
| AI integration | Google Gemini API | 1.5-flash / 1.5-pro |
| Reactive HTTP | Spring WebFlux / WebClient | 3.2.x |
| Container | Docker + Docker Compose | 24+ |
| Cloud target | Google Cloud Platform | — |

---

## 4. Prerequisites

| Tool | Version | Download |
|------|---------|---------|
| Java JDK | 21 | https://adoptium.net/ |
| Maven | 3.9 | https://maven.apache.org/download.cgi |
| Node.js | 18 | https://nodejs.org/ |
| Angular CLI | 17 | `npm install -g @angular/cli` |
| Docker *(optional)* | 24 | https://docs.docker.com/get-docker/ |

---

## 5. Quick Start

### Option A — Shell script

```bash
cd agileguard
./start-all.sh        # builds + starts all services and Angular
open http://localhost:4200
```

### Option B — Docker Compose

```bash
cd agileguard
docker compose up --build
open http://localhost:4200
```

### Option C — Manual

```bash
# Build
mvn clean package -DskipTests

# Start services (order matters — Auth before Gateway)
java -jar agileguard-auth-service/target/*.jar &
java -jar agileguard-jira-service/target/*.jar &
java -jar agileguard-ai-service/target/*.jar &
java -jar agileguard-github-service/target/*.jar &
java -jar agileguard-gateway/target/*.jar &

# Start Angular
cd agileguard-frontend && npm install && npm start
```

### Demo Login Credentials (auto-seeded on `local` profile)

| Role | Email | Password |
|------|-------|---------|
| Admin | admin@acme.com | Admin@1234 |
| Developer | dev@acme.com | Dev@1234 |
| QA Tester | qa@acme.com | Qa@1234 |

### Developer Console Links

| URL | Service | Notes |
|-----|---------|-------|
| http://localhost:4200 | Angular UI | Main application |
| http://localhost:8080 | API Gateway | All API calls proxied here |
| http://localhost:8081/h2-console | Auth H2 | JDBC: `jdbc:h2:mem:authdb` / user: `sa` |
| http://localhost:8082/h2-console | JIRA H2 | JDBC: `jdbc:h2:mem:jiradb` / user: `sa` |
| http://localhost:8081/actuator/health | Auth health | |
| http://localhost:8082/actuator/health | JIRA health | |
| http://localhost:8083/actuator/health | AI health | |
| http://localhost:8084/actuator/health | GitHub health | |

---

## 6. Module Structure

```
agileguard/
├── pom.xml                              Parent POM — Java 21, Spring Boot 3.2
├── README.md
├── ENV_VARIABLES.md                     All environment variable definitions
├── bigquery-ddl.sql                     Production BigQuery DDL
├── docker-compose.yml                   Single-command local Docker startup
├── start-all.sh / stop-all.sh / run-tests.sh
│
├── agileguard-common/                   Shared library (no runnable jar)
│   └── dto/     ApiResponse, ErrorResponse, PageResponse
│   └── enums/   Role, GapType, Severity, IssueType, IssueStatus
│   └── exception/ AgileGuardException
│   └── util/    TenantContext (ThreadLocal per-request isolation)
│
├── agileguard-gateway/          :8080   Spring Cloud Gateway
│   └── JwtGatewayFilter                JWT validation + X-Tenant-ID header injection
│
├── agileguard-auth-service/     :8081   Auth, Onboarding, Tenant Management
│   ├── controllers  AuthController, OnboardingController, AdminController
│   ├── services     AuthService, JwtService, OnboardingService,
│   │                TenantService, TenantManagementService
│   └── config       SecurityConfig (permits /api/onboarding/** publicly)
│                    DataInitializer (seeds demo data on 'local' profile)
│
├── agileguard-jira-service/     :8082   JIRA Integration + Gap Detection
│   ├── controllers  JiraController (issues, scan, lookups, create)
│   ├── services     JiraClientService, JiraLookupService,
│   │                StoryGapDetector (8 rules), TransitionGuard,
│   │                GapReportService
│   ├── models       JiraIssue (13 fields), JiraSprint, JiraComponent,
│   │                JiraVersion, JiraEpic, JiraUser, CreateIssueRequest
│   └── scheduler    GapScanScheduler (runs every 15 min)
│
├── agileguard-ai-service/       :8083   Google Gemini AI
│   ├── controllers  AiController (validate, generate-AC, SSE stream)
│   └── services     GeminiClientService, StoryValidationService
│
├── agileguard-github-service/   :8084   GitHub Actions + Leaderboard
│   ├── controllers  GitHubController
│   └── services     GitHubClientService, SdlcControlService, LeaderboardService
│
└── agileguard-frontend/         :4200   Angular 17 SPA
    └── src/app/
        ├── core/
        │   ├── models/      index.ts (all TypeScript interfaces)
        │   ├── services/    AuthService, JiraService, AiService,
        │   │                GitHubService, TenantService, ExportService
        │   ├── guards/      authGuard, adminGuard
        │   ├── interceptors/ jwtInterceptor
        │   └── pipes/       GapTypeLabelPipe
        ├── shared/
        │   └── components/  TypeaheadComponent, TagInputComponent
        └── features/
            ├── auth/        LoginComponent
            ├── onboarding/  OnboardingComponent (5-step wizard)
            ├── dashboard/   DashboardComponent
            ├── story-form/  StoryFormComponent (13 fields + AI)
            ├── reports/     ReportsComponent (filters + 6 export formats)
            ├── leaderboard/ LeaderboardComponent
            └── admin/       AdminComponent (4 tabs)
```

---

## 7. Frontend Pages

| Route | Guard | Component | Description |
|-------|-------|-----------|-------------|
| `/login` | Public | LoginComponent | JWT sign-in with demo credential hints |
| `/onboarding` | Public | OnboardingComponent | 5-step self-service tenant creation |
| `/dashboard` | Auth | DashboardComponent | Sprint health score, flagged stories, gap summary |
| `/stories` | Auth | StoryFormComponent | Full JIRA story editor with 13 fields + AI assist |
| `/reports` | Auth | ReportsComponent | Gap analysis, severity filters, CI/CD table, 6 export formats |
| `/leaderboard` | Auth | LeaderboardComponent | Composite score, badges, score breakdown |
| `/admin` | Admin | AdminComponent | 4-tab tenant/project/team/user management |

---

## 8. Complete API Reference

All calls route through the Gateway at `http://localhost:8080`.
Include `Authorization: Bearer <token>` on every protected endpoint.

### Authentication — `/api/auth`

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST | `/api/auth/register` | Public | Register user in an existing tenant |
| POST | `/api/auth/login` | Public | Login, returns accessToken + refreshToken |
| POST | `/api/auth/refresh` | Public | Exchange refresh token for new access token |

### Onboarding — `/api/onboarding`

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST | `/api/onboarding` | Public | One-shot tenant creation, returns JWT tokens |
| POST | `/api/onboarding/jira-test` | Public | Validate JIRA credentials before saving |
| GET | `/api/onboarding/check-slug/{slug}` | Public | Slug availability check (live debounce) |

### Tenant CRUD — `/api/tenants` and `/api/admin`

| Method | Endpoint | Roles | Purpose |
|--------|----------|-------|---------|
| GET | `/api/tenants` | SUPER_ADMIN | List all tenants |
| GET | `/api/tenants/{id}` | ADMIN | Tenant details |
| POST | `/api/tenants` | SUPER_ADMIN | Create tenant |
| PUT | `/api/admin/tenants/{id}` | TENANT_ADMIN+ | Update name, plan, JIRA config |
| DELETE | `/api/admin/tenants/{id}` | SUPER_ADMIN | Soft-deactivate |
| GET | `/api/tenants/{id}/projects` | Auth | List project teams |
| POST | `/api/tenants/{id}/projects` | TENANT_ADMIN+ | Create project team |
| PUT | `/api/admin/projects/{id}` | PROJECT_ADMIN+ | Update project team |
| DELETE | `/api/admin/projects/{id}` | TENANT_ADMIN+ | Soft-deactivate |
| GET | `/api/admin/projects/{id}/teams` | PROJECT_ADMIN+ | List feature teams |
| POST | `/api/admin/projects/{id}/teams` | PROJECT_ADMIN+ | Create feature team |
| PUT | `/api/admin/teams/{id}` | PROJECT_ADMIN+ | Update feature team |
| DELETE | `/api/admin/teams/{id}` | PROJECT_ADMIN+ | Soft-deactivate |

### User Management — `/api/admin/users`

| Method | Endpoint | Roles | Purpose |
|--------|----------|-------|---------|
| GET | `/api/admin/tenants/{id}/users` | PROJECT_ADMIN+ | List users in tenant |
| GET | `/api/admin/users/{id}` | PROJECT_ADMIN+ | Single user profile |
| PUT | `/api/admin/users/{id}/role` | TENANT_ADMIN+ | Update role and feature team |
| DELETE | `/api/admin/users/{id}` | TENANT_ADMIN+ | Soft-deactivate user |

### JIRA Integration — `/api/jira`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/jira/projects/{key}/issues` | Active sprint issues with scores |
| POST | `/api/jira/projects/{key}/scan` | Full gap scan → SprintHealthReport |
| GET | `/api/jira/issues/{key}` | Single issue detail |
| GET | `/api/jira/issues/{key}/gap-report` | Gap analysis for one issue |
| POST | `/api/jira/issues/{key}/validate-transition?toStatus=` | Transition gate check |
| POST | `/api/jira/issues/create` | Create issue with all 13 fields |
| GET | `/api/jira/gaps` | All open gap findings for tenant |
| GET | `/api/jira/projects/{key}/sprints?q=` | Typeahead — active + future sprints |
| GET | `/api/jira/projects/{key}/components?q=` | Typeahead — project components |
| GET | `/api/jira/projects/{key}/versions?q=` | Typeahead — fix versions |
| GET | `/api/jira/users?q=` | Typeahead — users by name or email |
| GET | `/api/jira/epics/{epicKey}` | Resolve epic key to name + status |

### Gemini AI — `/api/ai`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/ai/validate` | Full story validation, returns score + suggestions |
| GET | `/api/ai/validate/stream?title=&description=&ac=` | SSE streaming real-time validation |
| POST | `/api/ai/ac/generate` | Generate Given/When/Then Acceptance Criteria |

### GitHub / CI/CD — `/api/github`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/github/repos/{owner}/{repo}/runs` | GitHub Actions workflow runs |
| GET | `/api/github/sdlc/{jiraIssueKey}` | SDLC gate status for a story |
| GET | `/api/github/leaderboard/{projectId}` | Developer/QA leaderboard |

---

## 9. Gap Detection Engine

`StoryGapDetector` runs every 15 minutes via `GapScanScheduler` and evaluates every active sprint story against 8 rules.

### Quality Score

```
Start at 100
CRITICAL finding  −30 pts
ERROR finding     −15 pts
WARNING finding    −7 pts
Minimum score       0 pts

Story is FLAGGED when: score < 40 OR any CRITICAL finding
```

### Gap Rules

| Rule | Severity | Trigger | Deduction |
|------|----------|---------|-----------|
| Missing Description | ERROR | Blank or < 20 chars | −15 |
| Missing Acceptance Criteria | ERROR | AC field empty | −15 |
| Bad AC Format | WARNING | No Given/When/Then or bullets | −7 |
| Missing Story Points | WARNING | Story/Bug unestimated | −7 |
| Missing Dev Sub-task | WARNING | In Progress with no Dev Task | −7 |
| Missing QA Sub-task | WARNING | In QA stage with no QA Task | −7 |
| No Effort Logged | WARNING | In Progress, 0 minutes logged | −7 |
| **Aging Story** | **CRITICAL** | Same status > 5 days | **−30** |
| No PR Linked | WARNING | In Review, no pull request | −7 |

Configurable thresholds in `application.yml`:

```yaml
agileguard.jira.aging-threshold-days: 5
agileguard.jira.min-description-length: 20
```

---

## 10. Transition Guard Gates

| Transition | Required Gates |
|-----------|---------------|
| → Ready for QA | Dev sub-task exists · Effort > 0 minutes · PR linked |
| → Ready for Release | QA sub-task exists · AC present · Story points set |
| → Done | QA sub-task exists |

A failed gate returns HTTP 400 with every unmet condition listed by name.

---

## 11. Story Editor — Enhanced Fields

| Field | UI Control | Required | Behaviour |
|-------|-----------|----------|-----------|
| Issue Type | Dropdown | Yes | Story / Bug / Task |
| Priority | Dropdown | Yes | Blocker · Critical · Major · Medium · Minor · Low · Trivial |
| Business Line | Dropdown | Yes | Business · GT · Platform · Infrastructure · Data · Security |
| Summary | Text | Yes | Min 5 chars |
| Description | Textarea | Yes | Min 20 chars, What-and-Why format |
| Acceptance Criteria | Textarea | Yes | AI validates and generates Given/When/Then |
| Story Points | Dropdown | — | Fibonacci: 1 2 3 5 8 13 21 |
| Reporter | Typeahead single | — | Searches JIRA users by name or email |
| Sprint | Typeahead single | — | Active + future sprints, shows goal + end date |
| Epic Link | Text + Resolve | — | Types "PLAT-5" → fetches epic name on blur |
| Component/s | Typeahead multi | — | Tag chips, filtered project components |
| Fix Version/s | Typeahead multi | — | Tag chips with release dates |
| Labels | Tag input | — | Enter or comma to add free-text tags |
| Team Names | Pill toggle | — | Multi-select team pill grid |

The completion checklist tracks all 13 items. Flagged items show REQUIRED / OPTIONAL badges.

---

## 12. Tenant Onboarding Workflow

Self-service at `/onboarding` — no prior account required.

```
Step 1  Organisation   name, slug (debounced availability check), plan
Step 2  JIRA           base URL, email, API token + "Test Connection" button
Step 3  Teams          project team + add multiple feature teams inline
Step 4  Admin User     email, password with strength meter, GitHub/JIRA IDs
Step 5  Review         read-only summary → create atomically → JWT issued
```

On success: all entities created in one transaction, JWT returned, browser navigates to `/admin?welcome=1`.

---

## 13. Admin Panel

Four-tab management panel at `/admin` (requires PROJECT_ADMIN or higher).

| Tab | Actions |
|-----|---------|
| Overview | Edit name/plan inline; update JIRA credentials; view org stats |
| Projects | List / create / edit / deactivate project teams |
| Teams | Select project then list / create / deactivate feature teams |
| Members | View all users; update role inline via dropdown; deactivate |

---

## 14. Gemini AI Integration

| Environment | Mock | Model | Key source |
|-------------|------|-------|-----------|
| Local | Yes | — | Not required |
| Dev | Optional | gemini-1.5-flash | `GEMINI_API_KEY` env var |
| UAT | No | gemini-1.5-pro | `GEMINI_API_KEY` env var |
| Prod | No | gemini-1.5-pro | GCP Secret Manager |

To get an API key: https://aistudio.google.com/app/apikey

---

## 15. GitHub CI/CD Integration

SDLC gates checked when a story transitions to Ready for Release:

| Gate | Source | Pass |
|------|--------|------|
| Unit tests | GitHub Actions | All CI jobs: success |
| Integration tests | GitHub Actions | All integration jobs: success |
| Security scan | CodeQL / Trivy | No critical findings |
| PR merged | GitHub API | PR linked to story branch merged |
| PR reviewed | GitHub API | Required approvals met |

---

## 16. Developer Leaderboard

Composite score 0–100 per contributor per sprint:

| Factor | Weight |
|--------|--------|
| Story quality (avg AI score) | 30% |
| Effort logging compliance | 20% |
| Sub-task compliance | 15% |
| PR review contributions | 15% |
| CI/CD pass rate | 10% |
| Commit frequency | 10% |

---

## 17. Gap Reports and Export

Filters: severity tabs (All / Critical / Error / Warning / Clean) · free-text search · flagged-only toggle.

| Export | Output |
|--------|--------|
| CSV — All Gaps | One row per finding, summary at bottom |
| CSV — Flagged Only | Critical and high-risk stories |
| CSV — CI/CD Runs | GitHub Actions run history |
| JSON — Full Report | Complete SprintHealthReport object |
| HTML — Shareable | Self-contained, email-ready page |
| PDF | Browser print dialog → Save as PDF |

---

## 18. Multi-Environment Configuration

Each service has four Spring profile YAMLs:

```
application.yml         LOCAL  H2, mocks, DEBUG
application-dev.yml     DEV    PostgreSQL, real APIs switchable, DEBUG
application-uat.yml     UAT    PostgreSQL, real APIs, Flyway migrations, INFO
application-prod.yml    PROD   Cloud SQL, GCP secrets, Flyway, WARN
```

Activate:

```bash
java -Dspring.profiles.active=uat -jar service.jar
# or
export SPRING_PROFILES_ACTIVE=prod && java -jar service.jar
```

Angular build per environment:

```bash
npm run build:dev    # development configuration
npm run build:uat    # UAT configuration
npm run build:prod   # production (minified, no source maps)
```

---

## 19. RBAC — Roles and Permissions

| Role | Scope | Key Capabilities |
|------|-------|----------------|
| SUPER_ADMIN | Platform | All tenants, billing, global admin |
| TENANT_ADMIN | Tenant | Projects, users, JIRA config, deactivate |
| PROJECT_ADMIN | Project | Feature teams, rules, GitHub repos |
| FEATURE_LEAD | Team | Members, sprint planning |
| PRODUCT_OWNER | Project | Epics, stories, release approval |
| DEVELOPER | Team | Sub-tasks, effort logging, leaderboard |
| QA_TESTER | Team | QA tasks, test effort, leaderboard |
| VIEWER | Project | Read-only reports and boards |

---

## 20. Connecting to Real APIs

No code changes. Only YAML config:

```yaml
# JIRA — agileguard-jira-service application.yml
agileguard.jira.use-mock: false
agileguard.jira.base-url: https://YOUR-ORG.atlassian.net
agileguard.jira.user-email: svc@yourcompany.com
agileguard.jira.api-token: ATATT3x...

# Gemini — agileguard-ai-service application.yml
agileguard.gemini.use-mock: false
agileguard.gemini.api-key: AIzaSy...
agileguard.gemini.model: gemini-1.5-flash

# GitHub — agileguard-github-service application.yml
agileguard.github.use-mock: false
agileguard.github.token: ghp_...
```

---

## 21. Running Tests

```bash
# All Java tests
./run-tests.sh

# Individual services
mvn test -pl agileguard-auth-service
mvn test -pl agileguard-jira-service
mvn test -pl agileguard-ai-service
mvn test -pl agileguard-github-service

# Angular
cd agileguard-frontend
ng test --browsers=ChromeHeadless --watch=false
```

Test summary: 9 test classes · 77 total test cases across unit and integration tests.

---

## 22. Docker Deployment

See the Dockerfiles in each service directory and `docker-compose.yml` at the project root.

```bash
docker compose up --build          # start all
docker compose up -d               # background
docker compose logs -f gateway     # tail logs
docker compose down                # stop and remove
```

---

## 23. BigQuery Schema — Production

See `bigquery-ddl.sql` in the project root. Key tables:

| Table | Dataset | Purpose |
|-------|---------|---------|
| ag_tenants | core | Tenant registry |
| ag_project_teams | core | Project team hierarchy |
| ag_feature_teams | core | Feature team membership |
| ag_users | core | User accounts and role assignments |
| ag_gap_findings | reports | Individual gap findings per scan |
| ag_story_quality_log | reports | Per-issue quality scores per run |
| ag_sprint_health | reports | Sprint-level health aggregates |
| ag_github_runs | reports | GitHub Actions run history |
| ag_leaderboard_snapshots | reports | Per-sprint leaderboard scores |
| ag_audit_log | audit | Immutable audit trail |

All tables are partitioned by `tenant_id` for data isolation.
