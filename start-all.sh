#!/bin/bash
# AgileGuard — Start All Services (POC / Local)
# Usage: ./start-all.sh [--skip-build]
# Starts: Gateway (8080), Auth (8081), JIRA (8082), AI (8083), GitHub (8084), Angular (4200)
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
LOGS="$ROOT/.logs"
mkdir -p "$LOGS"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}[AgileGuard]${NC} $1"; }
success() { echo -e "${GREEN}[AgileGuard]${NC} $1"; }
warn()    { echo -e "${YELLOW}[AgileGuard]${NC} $1"; }

# ── Prerequisites ──────────────────────────────────────────────────────────────
check_prereqs() {
  info "Checking prerequisites..."
  java_ver=$(java -version 2>&1 | head -1 | grep -oP '(?<=version ")[0-9]+' || echo "0")
  if [ "$java_ver" -lt 21 ] 2>/dev/null; then
    echo -e "${RED}ERROR: Java 21+ required (found: $java_ver). Install from https://adoptium.net/${NC}"
    exit 1
  fi
  success "Java $java_ver found"

  if ! command -v mvn &>/dev/null; then
    echo -e "${RED}ERROR: Maven not found. Install from https://maven.apache.org/download.cgi${NC}"
    exit 1
  fi
  success "Maven $(mvn -version 2>&1 | head -1 | awk '{print $3}') found"

  if command -v node &>/dev/null && command -v ng &>/dev/null; then
    success "Node $(node -v) and Angular CLI found"
    ANGULAR_AVAILABLE=true
  else
    warn "Node/Angular CLI not found — Angular UI will not start"
    warn "Install from https://nodejs.org/ then run: npm install -g @angular/cli"
    ANGULAR_AVAILABLE=false
  fi
}

# ── Build ──────────────────────────────────────────────────────────────────────
build_services() {
  if [[ "$*" == *"--skip-build"* ]]; then
    warn "Skipping Maven build (--skip-build flag)"
    return
  fi
  info "Building all Maven modules (this takes ~2-3 minutes)..."
  cd "$ROOT"
  mvn clean package -DskipTests -q 2>&1 | tail -5
  success "Maven build complete"
}

# ── Start a Spring Boot service ───────────────────────────────────────────────
start_service() {
  local name=$1 jar=$2 port=$3
  if [ ! -f "$jar" ]; then
    warn "JAR not found: $jar — skipping $name"
    return
  fi
  info "Starting $name on port $port..."
  nohup java -jar "$jar" > "$LOGS/$name.log" 2>&1 &
  echo $! > "$LOGS/$name.pid"
  success "$name started (PID $(cat $LOGS/$name.pid)) → http://localhost:$port"
}

# ── Wait for service ──────────────────────────────────────────────────────────
wait_for() {
  local name=$1 port=$2
  local retries=300
  while [ $retries -gt 0 ]; do
    if curl -sf "http://localhost:$port/actuator/health" &>/dev/null; then
      success "$name is healthy ✓"
      return
    fi
    sleep 2; ((retries--))
  done
  warn "$name did not become healthy in time — check $LOGS/$name.log"
}

# ── Main ───────────────────────────────────────────────────────────────────────
echo ""
echo "  ╔═══════════════════════════════════╗"
echo "  ║   🛡️  AgileGuard  Platform         ║"
echo "  ║   SDLC Governance — Local POC     ║"
echo "  ╚═══════════════════════════════════╝"
echo ""

check_prereqs
build_services "$@"

# Start microservices
start_service "auth-service"   "$ROOT/agileguard-auth-service/target/agileguard-auth-service-1.0.0-SNAPSHOT.jar" 8081
sleep 5
wait_for "auth-service" 8081

start_service "jira-service"   "$ROOT/agileguard-jira-service/target/agileguard-jira-service-1.0.0-SNAPSHOT.jar" 8082
start_service "ai-service"     "$ROOT/agileguard-ai-service/target/agileguard-ai-service-1.0.0-SNAPSHOT.jar"   8083
start_service "github-service" "$ROOT/agileguard-github-service/target/agileguard-github-service-1.0.0-SNAPSHOT.jar" 8084
sleep 5
start_service "gateway"        "$ROOT/agileguard-gateway/target/agileguard-gateway-1.0.0-SNAPSHOT.jar" 8080

if [ "$ANGULAR_AVAILABLE" = true ]; then
  info "Starting Angular frontend on port 4200..."
  cd "$ROOT/agileguard-frontend"
  if [ ! -d "node_modules" ]; then
    info "Installing npm packages (first run — this may take a minute)..."
    npm install --silent
  fi
  nohup ng serve --proxy-config proxy.conf.local.json > "$LOGS/angular.log" 2>&1 &
  echo $! > "$LOGS/angular.pid"
  success "Angular started → http://localhost:4200"
fi

echo ""
echo "  ╔══════════════════════════════════════════════════════════════╗"
echo "  ║  AgileGuard is running!                                      ║"
echo "  ║                                                              ║"
echo "  ║  Angular UI:  http://localhost:4200                          ║"
echo "  ║  API Gateway: http://localhost:8080                          ║"
echo "  ║  Auth:        http://localhost:8081  (H2: /h2-console)       ║"
echo "  ║  JIRA:        http://localhost:8082                          ║"
echo "  ║  AI:          http://localhost:8083                          ║"
echo "  ║  GitHub:      http://localhost:8084                          ║"
echo "  ║                                                              ║"
echo "  ║  Demo logins: admin@db.com / Admin@1234                      ║"
echo "  ║               dev@db.com   / Dev@1234                        ║"
echo "  ║               qa@db.com    / Qa@1234                         ║"
echo "  ║                                                              ║"
echo "  ║  Logs: .logs/<service>.log                                   ║"
echo "  ║  Stop: ./stop-all.sh                                         ║"
echo "  ╚══════════════════════════════════════════════════════════════╝"
echo ""
