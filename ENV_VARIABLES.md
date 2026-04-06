# AgileGuard — Environment Variables Reference

All secrets are injected via environment variables.
**Never hardcode secrets in any YAML file committed to source control.**

---

## How to activate a profile

### Spring Boot (all services)
```bash
# Option A — JVM system property (recommended for Docker/K8s)
java -Dspring.profiles.active=dev -jar service.jar

# Option B — Environment variable (recommended for Cloud Run / GKE)
export SPRING_PROFILES_ACTIVE=dev
java -jar service.jar

# Option C — Docker Compose / Kubernetes env block
environment:
  SPRING_PROFILES_ACTIVE: "uat"
```

### Angular
```bash
npm run start          # local  — proxied to localhost:8080
npm run start:dev      # dev    — proxied to dev API gateway
npm run start:uat      # uat    — proxied to UAT API gateway

npm run build:dev      # production bundle with dev environment
npm run build:uat      # production bundle with UAT environment
npm run build:prod     # production bundle with prod environment
```

---

## Environment variable catalogue

### Shared across services (all non-local environments)

| Variable | Required | Example | Description |
|----------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | ✅ | `dev` | Activates the matching `application-{profile}.yml` |
| `AGILEGUARD_JWT_SECRET` | ✅ | `base64-256bit-key` | HS256 signing secret. Must match across gateway + auth service. Generate with: `openssl rand -base64 32` |

### Database (auth-service, jira-service — dev / UAT / prod)

| Variable | Required | Example | Description |
|----------|----------|---------|-------------|
| `DB_HOST` | ✅ (dev/uat) | `10.0.0.5` | PostgreSQL host (IP or hostname) |
| `DB_PORT` | ❌ | `5432` | Defaults to 5432 |
| `DB_NAME` | ✅ | `agileguard_dev` | Database name |
| `DB_USERNAME` | ✅ | `agileguard_app` | DB user with read/write on agileguard schema |
| `DB_PASSWORD` | ✅ | `s3cr3t!` | DB user password |
| `CLOUD_SQL_INSTANCE` | ✅ (prod) | `project:region:instance` | GCP Cloud SQL connection name (prod only) |

### JIRA integration (jira-service)

| Variable | Required | Example | Description |
|----------|----------|---------|-------------|
| `JIRA_USE_MOCK` | ❌ | `false` | Override mock flag at runtime |
| `JIRA_BASE_URL` | ✅ | `https://db.atlassian.net` | Base URL of the Atlassian instance |
| `JIRA_USER_EMAIL` | ✅ | `svc-agileguard@db.com` | Service account email for API auth |
| `JIRA_API_TOKEN` | ✅ | `ATATT3x...` | Atlassian API token (not password). Create at id.atlassian.com |
| `JIRA_SCAN_PROJECTS` | ✅ | `PLAT,SHOP,INFRA` | Comma-separated JIRA project keys to scan |
| `JIRA_SCAN_TENANT` | ✅ | `db-tenant-id` | Tenant ID to scope gap findings |
| `JIRA_AGING_THRESHOLD` | ❌ | `5` | Days before a story is marked aging (default: 5) |

### Gemini AI (ai-service)

| Variable | Required | Example | Description |
|----------|----------|---------|-------------|
| `GEMINI_USE_MOCK` | ❌ | `false` | Override mock flag at runtime |
| `GEMINI_API_KEY` | ✅ | `AIzaSy...` | Google Gemini API key from aistudio.google.com |
| `GEMINI_MODEL` | ❌ | `gemini-1.5-pro` | Model to use (default per profile) |

### GitHub integration (github-service)

| Variable | Required | Example | Description |
|----------|----------|---------|-------------|
| `GITHUB_USE_MOCK` | ❌ | `false` | Override mock flag at runtime |
| `GITHUB_TOKEN` | ✅ | `ghp_...` | GitHub Personal Access Token or GitHub App token. Scopes: `repo`, `workflow`, `read:org` |

---

## Generating a secure JWT secret

```bash
# Generate a 256-bit (32-byte) random secret encoded as Base64
openssl rand -base64 32

# Example output (use this as AGILEGUARD_JWT_SECRET):
# K8J2mN7pQ3rX9vY1wZ5aB6cD0eF4gH8iL2mN7pQ3r=

# The SAME value must be set on BOTH:
#   agileguard-gateway   (validates tokens)
#   agileguard-auth-service  (signs tokens)
```

---

## Local .env file (for docker-compose local dev)

Create a `.env` file at the project root (git-ignored):

```dotenv
# .env — local docker-compose overrides
SPRING_PROFILES_ACTIVE=dev

AGILEGUARD_JWT_SECRET=K8J2mN7pQ3rX9vY1wZ5aB6cD0eF4gH8iL2mN7pQ3r=

DB_HOST=localhost
DB_PORT=5432
DB_NAME=agileguard_dev
DB_USERNAME=agileguard
DB_PASSWORD=localpassword

JIRA_USE_MOCK=false
JIRA_BASE_URL=https://your-org.atlassian.net
JIRA_USER_EMAIL=you@example.com
JIRA_API_TOKEN=ATATT3xFfGF...
JIRA_SCAN_PROJECTS=PLAT
JIRA_SCAN_TENANT=your-tenant-id

GEMINI_USE_MOCK=false
GEMINI_API_KEY=AIzaSy...

GITHUB_USE_MOCK=false
GITHUB_TOKEN=ghp_...
```

> Add `.env` to `.gitignore` immediately. Never commit it.

---

## GCP Secret Manager (UAT / PROD)

For Cloud Run deployments, secrets are mounted as environment variables:

```yaml
# Cloud Run service YAML snippet
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
  - name: AGILEGUARD_JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: agileguard-jwt-secret
        key: latest
  - name: JIRA_API_TOKEN
    valueFrom:
      secretKeyRef:
        name: jira-api-token
        key: latest
  - name: GEMINI_API_KEY
    valueFrom:
      secretKeyRef:
        name: gemini-api-key
        key: latest
  - name: GITHUB_TOKEN
    valueFrom:
      secretKeyRef:
        name: github-token
        key: latest
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: db-password
        key: latest
```

Create secrets via gcloud CLI:
```bash
echo -n "YOUR_SECRET_VALUE" | gcloud secrets create jira-api-token \
  --data-file=- --replication-policy=automatic
```

---

## Profile behaviour summary

| Profile | DB | JIRA | Gemini | GitHub | Log Level | H2 Console | Source Maps |
|---------|-----|------|--------|--------|-----------|------------|-------------|
| `local` | H2 in-memory | Mock | Mock | Mock | DEBUG | ✅ Enabled | ✅ Yes |
| `dev` | PostgreSQL | Real* | Real* | Real* | DEBUG | ❌ Off | ✅ Yes |
| `uat` | PostgreSQL | Real | Real | Real | INFO | ❌ Off | ✅ Yes |
| `prod` | Cloud SQL | Real | Real | Real | WARN/INFO | ❌ Off | ❌ No |

*Dev: `use-mock` defaults controlled per-service via env var — can switch without redeploy.

